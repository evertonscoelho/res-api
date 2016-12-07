package br.ufsc.bridge.res.service.registry;

import java.net.MalformedURLException;
import java.util.ArrayList;

import javax.xml.bind.JAXBElement;

import lombok.extern.slf4j.Slf4j;

import br.ufsc.bridge.res.dab.exception.ResABXMLParserException;
import br.ufsc.bridge.res.http.ResHttpClient;
import br.ufsc.bridge.res.service.builder.SlotTypeBuilder.SlotTypeBuilderWrapper;
import br.ufsc.bridge.res.service.dto.header.Credential;
import br.ufsc.bridge.res.service.dto.header.RegistryHeader;
import br.ufsc.bridge.res.service.dto.registry.RegistryFilter;
import br.ufsc.bridge.res.service.dto.registry.RegistryItem;
import br.ufsc.bridge.res.service.dto.registry.RegistryResponse;
import br.ufsc.bridge.res.service.registry.parse.RegistryResponseParser;
import br.ufsc.bridge.res.service.repository.RepositoryService;
import br.ufsc.bridge.res.util.RDateUtil;
import br.ufsc.bridge.res.util.ResLogError;

import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.query._3.ResponseOptionType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AdhocQueryType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectRefType;

@Slf4j
public class RegistryService {

	private static final String SUCCESS = "urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success";
	public static final String SERVICO_RES_INDISPONIVEL = "Serviço RES-nacional não disponínel. Tente novamente.";

	private ResLogError printerResponseError;
	private ResHttpClient httpClient;

	public RegistryService(Credential c) {
		this.httpClient = new ResHttpClient(new RegistryHeader(c), "urn:ihe:iti:2007:ns:AdhocQueryRequestRequest");
		try {
			this.httpClient.setUrl("https://servicoshm.saude.gov.br/EHR-UNB/ProxyService/RegistryPS");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.printerResponseError = new ResLogError();
	}

	public RegistryResponse<RegistryItem> getRegistriesHeader(RegistryFilter filter) throws ResABXMLParserException {
		AdhocQueryResponse queryResponse = null;
		try {
			queryResponse = this.httpClient.send(this.buildRequest(filter, "LeafClass"), AdhocQueryResponse.class);
		} catch (Exception e) {
			log.error("erro no request", e);
			throw new ResABXMLParserException(SERVICO_RES_INDISPONIVEL, e);
		}

		if (queryResponse.getStatus().equals(SUCCESS)) {
			return RegistryResponseParser.parse(queryResponse);
		} else {
			this.printerResponseError.printLogError(queryResponse.getRegistryErrorList());
			throw new ResABXMLParserException(queryResponse.getRegistryErrorList());
		}
	}

	@SuppressWarnings("rawtypes")
	public RegistryResponse<String> getRegistriesRef(RegistryFilter filter) throws ResABXMLParserException {
		AdhocQueryResponse queryResponse = null;
		try {
			queryResponse = this.httpClient.send(this.buildRequest(filter, "ObjectRef"), AdhocQueryResponse.class);
		} catch (Exception e) {
			log.error("erro no request", e);
			throw new ResABXMLParserException(RepositoryService.SERVICO_RES_INDISPONIVEL, e);
		}

		if (queryResponse.getStatus().equals(SUCCESS)) {
			ArrayList<String> uuids = new ArrayList<>();
			for (JAXBElement jaxbElement : queryResponse.getRegistryObjectList().getIdentifiable()) {
				Object value = jaxbElement.getValue();
				if (value instanceof ObjectRefType) {
					uuids.add(((ObjectRefType) value).getId());
				}
			}
			return new RegistryResponse<>(uuids);
		} else {
			this.printerResponseError.printLogError(queryResponse.getRegistryErrorList());
			throw new ResABXMLParserException(queryResponse.getRegistryErrorList());
		}
	}

	private AdhocQueryRequest buildRequest(RegistryFilter filter, String tipoBusca) {
		ResponseOptionType responseOptionType = new ResponseOptionType();
		responseOptionType.setReturnComposedObjects(true);
		responseOptionType.setReturnType(tipoBusca);

		AdhocQueryType queryType = new AdhocQueryType();
		if (filter.hasEntryUUID()) {
			queryType.setId("urn:uuid:5c4f972b-d56b-40ac-a5fc-c8ca9b40b9d4");
		} else {
			queryType.setId("urn:uuid:14d4debf-8f97-4251-9a74-a90016b0af0d");
		}

		//@formatter:off
		new SlotTypeBuilderWrapper<>(queryType.getSlot())
				.name("$XDSDocumentEntryPatientId")
				.value("'" + filter.getCnsCidadao() + "^^^&2.16.840.1.113883.13.236&ISO'")
			.addSlotIf(filter.hasCnsCidadao())
				.name("$XDSDocumentEntryCreationTimeFrom")
				.value(RDateUtil.dateToISOXDSb(filter.getDataInicial()))
			.addSlotIf(filter.hasDataInicial())
				.name("$XDSDocumentEntryCreationTimeTo")
				.value(RDateUtil.dateToISOXDSb(filter.getDataFim()))
			.addSlotIf(filter.hasDataFim())
				.name("$XDSDocumentEntryEntryUUID")
				.value(filter.getEntryUUIDs(), true)
			.addSlotIf(filter.hasEntryUUID())
				.name("$XDSDocumentEntryStatus")
				.value("('urn:oasis:names:tc:ebxml-regrep:StatusType:Approved')")
			.addSlot();
		//@formatter:on

		AdhocQueryRequest adhocQueryRequest = new AdhocQueryRequest();
		adhocQueryRequest.setResponseOption(responseOptionType);
		adhocQueryRequest.setAdhocQuery(queryType);
		return adhocQueryRequest;
	}
}
