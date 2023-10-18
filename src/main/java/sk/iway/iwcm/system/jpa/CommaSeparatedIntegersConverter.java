package sk.iway.iwcm.system.jpa;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * Konverter pre databazovy zaznam kde su ciarkou oddelene cisla na objekt Integer[]
 * Pouziva sa napr. na perex_group v documents tabulke a podobne
 * POZOR: ak je zoznam neprazdny dava na zaciatok a koniec znak , pre lepsie hladanie ID podla like %,ID,%
 */
@Converter
public class CommaSeparatedIntegersConverter implements AttributeConverter<Integer[], String>{

    @Override
    public String convertToDatabaseColumn(Integer[] integers) {
        if (integers == null || integers.length<1) return null;

        StringBuilder str = new StringBuilder();
        for (Integer i : integers) {
            if (i != null) {
                str.append(",").append(String.valueOf(i));
            }
        }

        if (str.length()<1) return null;

        //pridaj koncovu ciarku
        str.append(",");

        return str.toString();
    }

    @Override
    public Integer[] convertToEntityAttribute(String ids) {

        if (ids == null) return new Integer[0];

        List<Integer> integersList = new ArrayList<>();
        String[] idsArray = ids.split(",");
        for (String id : idsArray) {
            try {
                Integer i = Integer.parseInt(id.trim());
                if (i != null) integersList.add(i);
            } catch (Exception ex) {
                //nie je cislo
            }
        }

        return integersList.toArray(new Integer[0]);
    }

}
