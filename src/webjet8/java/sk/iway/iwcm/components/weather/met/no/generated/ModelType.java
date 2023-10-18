package sk.iway.iwcm.components.weather.met.no.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for modelType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="modelType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="termin" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="runended" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="nextrun" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="from" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="to" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "modelType")
public class ModelType {

    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "termin")
    protected String termin;
    @XmlAttribute(name = "runended")
    protected String runended;
    @XmlAttribute(name = "nextrun")
    protected String nextrun;
    @XmlAttribute(name = "from")
    protected String from;
    @XmlAttribute(name = "to")
    protected String to;

    /**
     * Gets the value of the name property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setName(String value) {
        this.name = value;
    }

    /**
     * Gets the value of the termin property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTermin() {
        return termin;
    }

    /**
     * Sets the value of the termin property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTermin(String value) {
        this.termin = value;
    }

    /**
     * Gets the value of the runended property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRunended() {
        return runended;
    }

    /**
     * Sets the value of the runended property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRunended(String value) {
        this.runended = value;
    }

    /**
     * Gets the value of the nextrun property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getNextrun() {
        return nextrun;
    }

    /**
     * Sets the value of the nextrun property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setNextrun(String value) {
        this.nextrun = value;
    }

    /**
     * Gets the value of the from property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the value of the from property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFrom(String value) {
        this.from = value;
    }

    /**
     * Gets the value of the to property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTo() {
        return to;
    }

    /**
     * Sets the value of the to property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTo(String value) {
        this.to = value;
    }

}
