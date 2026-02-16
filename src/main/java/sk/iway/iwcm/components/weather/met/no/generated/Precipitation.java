package sk.iway.iwcm.components.weather.met.no.generated;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * Element denoting the precipitation in mm.
 *
 *
 * <p>Java class for precipitation complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType name="precipitation">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="unit" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="value" use="required">
 *         &lt;simpleType>
 *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}decimal">
 *             &lt;minInclusive value="0"/>
 *           &lt;/restriction>
 *         &lt;/simpleType>
 *       &lt;/attribute>
 *       &lt;attribute name="minvalue" type="{http://www.w3.org/2001/XMLSchema}float" />
 *       &lt;attribute name="maxvalue" type="{http://www.w3.org/2001/XMLSchema}float" />
 *       &lt;attribute name="probability" type="{http://www.w3.org/2001/XMLSchema}float" />
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "precipitation")
public class Precipitation {

    @XmlAttribute(name = "unit", required = true)
    protected String unit;
    @XmlAttribute(name = "value", required = true)
    protected BigDecimal value;
    @XmlAttribute(name = "minvalue")
    protected Float minvalue;
    @XmlAttribute(name = "maxvalue")
    protected Float maxvalue;
    @XmlAttribute(name = "probability")
    protected Float probability;
    @XmlAttribute(name = "id")
    protected String id;

    /**
     * Gets the value of the unit property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Sets the value of the unit property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setUnit(String value) {
        this.unit = value;
    }

    /**
     * Gets the value of the value property.
     *
     * @return
     *     possible object is
     *     {@link BigDecimal }
     *
     */
    public BigDecimal getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value
     *     allowed object is
     *     {@link BigDecimal }
     *
     */
    public void setValue(BigDecimal value) {
        this.value = value;
    }

    /**
     * Gets the value of the minvalue property.
     *
     * @return
     *     possible object is
     *     {@link Float }
     *
     */
    public Float getMinvalue() {
        return minvalue;
    }

    /**
     * Sets the value of the minvalue property.
     *
     * @param value
     *     allowed object is
     *     {@link Float }
     *
     */
    public void setMinvalue(Float value) {
        this.minvalue = value;
    }

    /**
     * Gets the value of the maxvalue property.
     *
     * @return
     *     possible object is
     *     {@link Float }
     *
     */
    public Float getMaxvalue() {
        return maxvalue;
    }

    /**
     * Sets the value of the maxvalue property.
     *
     * @param value
     *     allowed object is
     *     {@link Float }
     *
     */
    public void setMaxvalue(Float value) {
        this.maxvalue = value;
    }

    /**
     * Gets the value of the probability property.
     *
     * @return
     *     possible object is
     *     {@link Float }
     *
     */
    public Float getProbability() {
        return probability;
    }

    /**
     * Sets the value of the probability property.
     *
     * @param value
     *     allowed object is
     *     {@link Float }
     *
     */
    public void setProbability(Float value) {
        this.probability = value;
    }

    /**
     * Gets the value of the id property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setId(String value) {
        this.id = value;
    }

}
