package br.ufsc.bridge.res.dab.dto;

import javax.xml.xpath.XPathExpressionException;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import br.ufsc.bridge.res.dab.domain.ResABTipoProblemaDiagnostico;
import br.ufsc.bridge.res.util.XPathFactoryAssist;

@Getter
@Setter
@NoArgsConstructor
public class ResABProblemaDiagnostico {
	private String descricao;
	private ResABTipoProblemaDiagnostico tipo;
	private String codigo;

	public ResABProblemaDiagnostico(XPathFactoryAssist xPathDiagnostico) throws XPathExpressionException {
		this.codigo = xPathDiagnostico.getString("./data/Problema__fslash__Diagnóstico/value/defining_code/code_string");
		this.descricao = xPathDiagnostico.getString("./data/Problema__fslash__Diagnóstico/value/value");
		this.tipo = ResABTipoProblemaDiagnostico.getByTipo(xPathDiagnostico.getString("./data/Problema__fslash__Diagnóstico/value/defining_code/terminology_id/value"));
	}
}
