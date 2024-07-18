package io.mosip.kernel.emailnotification.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 
 * @author Alok
 * @since 1.0.0
 */
@Data


public class TemplateResponseDto implements Serializable {
	/**
	 * Default serial version
	 */
	private static final long serialVersionUID = 1L;
	private List<TemplateDto> templates;
}
