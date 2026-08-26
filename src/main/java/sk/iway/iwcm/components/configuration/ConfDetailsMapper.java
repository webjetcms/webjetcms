package sk.iway.iwcm.components.configuration;

import org.springframework.stereotype.Component;

import sk.iway.iwcm.components.configuration.model.ConfDetailsDto;
import sk.iway.iwcm.components.configuration.model.ConfPrefixDto;
import sk.iway.iwcm.system.ConfDetails;
import sk.iway.iwcm.system.spring.NullAwareBeanUtils;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class ConfDetailsMapper {

    private static final long JAVASCRIPT_MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

    /* ConfDetails TO ConfDetailsDto*/
    List<ConfDetailsDto> entityListToDtoList(List<ConfDetails> confDetails) {
        List<ConfDetailsDto> confDetailsDtos = new ArrayList<>();

        for (ConfDetails cd : confDetails) {
            ConfDetailsDto dto = entityToDto(cd);
            dto.setId(getStableId(cd.getName()));
            confDetailsDtos.add(dto);
        }
        return confDetailsDtos;
    }

    private long getStableId(String name) {
        UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
        long id = uuid.getMostSignificantBits() & JAVASCRIPT_MAX_SAFE_INTEGER;
        return id > 0 ? id : 1;
    }

    ConfDetailsDto entityToDto(ConfDetails cd) {
        ConfDetailsDto dto = new ConfDetailsDto();
        if (cd != null) NullAwareBeanUtils.copyProperties(cd, dto);
        return dto;
    }

    /* ConfDetails TO ConfTestDto*/
    List<ConfPrefixDto> entityListToPrefixDtoList(List<ConfDetails> confDetails) {
        long counter = 0;
        List<ConfPrefixDto> confPrefixDtos = new ArrayList<>();

        for (ConfDetails cd : confDetails) {
            ConfPrefixDto dto = entityToPrefixDto(cd);
            counter++;
            dto.setId(counter);
            confPrefixDtos.add(dto);
        }
        return confPrefixDtos;
    }

    ConfPrefixDto entityToPrefixDto(ConfDetails cd) {
        ConfPrefixDto dto = new ConfPrefixDto();
        NullAwareBeanUtils.copyProperties(cd, dto);
        return dto;
    }
}
