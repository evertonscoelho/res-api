package br.ufsc.bridge.res.registry;

import gov.nist.registry.ws.serviceclasses.Xdsregistryb;
import gov.nist.registry.ws.serviceclasses.XdsregistrybPortType;
import lombok.extern.slf4j.Slf4j;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryRequest;
import oasis.names.tc.ebxml_regrep.xsd.query._3.AdhocQueryResponse;
import oasis.names.tc.ebxml_regrep.xsd.query._3.ResponseOptionType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.AdhocQueryType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;
import br.ufsc.bridge.res.dto.header.Credential;
import br.ufsc.bridge.res.dto.header.RegistryHeader;
import br.ufsc.bridge.res.dto.registry.RegistryFilter;
import br.ufsc.bridge.res.dto.registry.RegistryResponse;
import br.ufsc.bridge.res.registry.parse.RegistryResponseParser;
import br.ufsc.bridge.res.util.RDateUtil;

@Slf4j
public class RegistryService {

	private static final String SUCCESS = "urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Success";
	// private static final String FAILURE = "urn:oasis:names:tc:ebxml-regrep:ResponseStatusType:Failure";

	private XdsregistrybPortType endpoint;

	public RegistryService(Credential c) {
		Xdsregistryb xdsregistryb = new Xdsregistryb();
		xdsregistryb.setHandlerResolver(new RegistryHeader(c));

		this.endpoint = xdsregistryb.getXdsregistrybHttpSoap12Endpoint();
	}

	public RegistryResponse getRegistries(RegistryFilter filter) {
		AdhocQueryResponse queryResponse = this.endpoint.adhocQueryRequest(this.buildRequest(filter));

		if (queryResponse.getStatus().equals(SUCCESS)) {
			return RegistryResponseParser.parse(queryResponse);
		} else {
			log.error("erro no request");
		}
		return null; // TODO TRATAR RETORNO NULO E FALHA
	}

	private AdhocQueryRequest buildRequest(RegistryFilter filter) {
		ResponseOptionType responseOptionType = new ResponseOptionType();
		responseOptionType.setReturnComposedObjects(true);
		responseOptionType.setReturnType("LeafClass");

		AdhocQueryType queryType = new AdhocQueryType();
		queryType.setId("urn:uuid:14d4debf-8f97-4251-9a74-a90016b0af0d");
		queryType.getSlot().add(getSlot("$XDSDocumentEntryPatientId", "'" + filter.getCnsCidadao() + "^^^&2.16.840.1.113883.13.236&ISO'"));
		if (filter.hasDataInicial()) {
			queryType.getSlot().add(getSlot("$XDSDocumentEntryCreationTimeFrom", RDateUtil.fromDate(filter.getDataInicial())));
		}
		if (filter.hasDataFim()) {
			queryType.getSlot().add(getSlot("$XDSDocumentEntryCreationTimeTo", RDateUtil.fromDate(filter.getDataFim())));
		}
		queryType.getSlot().add(getSlot("$XDSDocumentEntryStatus", "('urn:oasis:names:tc:ebxml-regrep:StatusType:Approved')"));

		AdhocQueryRequest adhocQueryRequest = new AdhocQueryRequest();
		adhocQueryRequest.setResponseOption(responseOptionType);
		adhocQueryRequest.setAdhocQuery(queryType);
		return adhocQueryRequest;
	}

	private static SlotType1 getSlot(String name, String value) {
		ValueListType listType = new ValueListType();
		listType.getValue().add(value);

		SlotType1 slotType1 = new SlotType1();
		slotType1.setName(name);
		slotType1.setValueList(listType);
		return slotType1;
	}

}
