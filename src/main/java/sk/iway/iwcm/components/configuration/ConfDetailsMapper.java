package sk.iway.iwcm.components.configuration;

import org.springframework.stereotype.Component;

import sk.iway.iwcm.components.configuration.model.ConfDetailsDto;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.spring.NullAwareBeanUtils;

import java.util.ArrayList;
import java.util.List;

@Component
class ConfDetailsMapper {

    List<ConfDetailsDto> entityListToDtoList(List<ConfDetails> confDetails) {
        long counter = 0;
        List<ConfDetailsDto> confDetailsDtos = new ArrayList<>();

        for (ConfDetails cd : confDetails) {
            ConfDetailsDto dto = entityToDto(cd);
            counter++;
            dto.setId(counter);
            confDetailsDtos.add(dto);
        }
        return confDetailsDtos;
    }

    ConfDetailsDto entityToDto(ConfDetails cd) {
        ConfDetailsDto dto = new ConfDetailsDto();
        NullAwareBeanUtils.copyProperties(cd, dto);
        return dto;
    }
}
