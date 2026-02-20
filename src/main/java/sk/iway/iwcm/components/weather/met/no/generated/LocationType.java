package sk.iway.iwcm.components.weather.met.no.generated;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElementRef;
import jakarta.xml.bind.annotation.XmlElementRefs;
import jakarta.xml.bind.annotation.XmlType;


/**
 * Element containing weather parameters for this location
 * 			
 * 
 * <p>Java class for locationType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="locationType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence maxOccurs="unbounded">
 *         &lt;element name="groundCover" type="{}groundcover" minOccurs="0"/>
 *         &lt;element name="pressure" type="{}pressure" minOccurs="0"/>
 *         &lt;element name="maximumPrecipitation" type="{}precipitation" minOccurs="0"/>
 *         &lt;element name="highestTemperature" type="{}temperature" minOccurs="0"/>
 *         &lt;element name="lowestTemperature" type="{}temperature" minOccurs="0"/>
 *         &lt;element name="precipitation" type="{}precipitation" minOccurs="0"/>
 *         &lt;element name="fog" type="{}cloudiness" minOccurs="0"/>
 *         &lt;element name="cloudiness" type="{}cloudiness" minOccurs="0"/>
 *         &lt;element name="lowClouds" type="{}cloudiness" minOccurs="0"/>
 *         &lt;element name="mediumClouds" type="{}cloudiness" minOccurs="0"/>
 *         &lt;element name="highClouds" type="{}cloudiness" minOccurs="0"/>
 *         &lt;element name="temperature" type="{}temperature" minOccurs="0"/>
 *         &lt;element name="dewpointTemperature" type="{}temperature" minOccurs="0"/>
 *         &lt;element name="minTemperature" type="{}temperature" minOccurs="0"/>
 *         &lt;element name="minTemperatureDay" type="{}temperature" minOccurs="0"/>
 *         &lt;element name="minTemperatureNight" type="{}temperature" minOccurs="0"/>
 *         &lt;element name="maxTemperature" type="{}temperature" minOccurs="0"/>
 *         &lt;element name="maxTemperatureDay" type="{}temperature" minOccurs="0"/>
 *         &lt;element name="maxTemperatureNight" type="{}temperature" minOccurs="0"/>
 *         &lt;element name="uv" type="{}uv" minOccurs="0"/>
 *         &lt;element name="tidalwater" type="{}tidalwater" minOccurs="0"/>
 *         &lt;element name="currentDirection" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="maxWaveHeight" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="surfaceTemperature" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="waveDirection" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="wavePeriod" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="waveHeight" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="humidity" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="bias" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="numberofobservations" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="meanabsoluteerror" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="score" type="{}score" minOccurs="0"/>
 *         &lt;element name="windDirection" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="deg" use="required">
 *                   &lt;simpleType>
 *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}decimal">
 *                       &lt;minInclusive value="0"/>
 *                       &lt;maxInclusive value="990"/>
 *                     &lt;/restriction>
 *                   &lt;/simpleType>
 *                 &lt;/attribute>
 *                 &lt;attribute name="name">
 *                   &lt;simpleType>
 *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                     &lt;/restriction>
 *                   &lt;/simpleType>
 *                 &lt;/attribute>
 *                 &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="windSpeed" type="{}windspeed" minOccurs="0"/>
 *         &lt;element name="windGust" type="{}windspeed" minOccurs="0"/>
 *         &lt;element name="maxWindSpeed" type="{}windspeed" minOccurs="0"/>
 *         &lt;element name="areaMaxWindSpeed" type="{}windspeed" minOccurs="0"/>
 *         &lt;element name="stateOfTheSea" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="number" use="required">
 *                   &lt;simpleType>
 *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
 *                       &lt;minInclusive value="0"/>
 *                       &lt;maxInclusive value="9"/>
 *                     &lt;/restriction>
 *                   &lt;/simpleType>
 *                 &lt;/attribute>
 *                 &lt;attribute name="meter" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="snowDepth" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="cm" use="required">
 *                   &lt;simpleType>
 *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}decimal">
 *                       &lt;minInclusive value="0"/>
 *                     &lt;/restriction>
 *                   &lt;/simpleType>
 *                 &lt;/attribute>
 *                 &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="weather" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="name">
 *                   &lt;simpleType>
 *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *                     &lt;/restriction>
 *                   &lt;/simpleType>
 *                 &lt;/attribute>
 *                 &lt;attribute name="number" use="required">
 *                   &lt;simpleType>
 *                     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
 *                       &lt;minInclusive value="0"/>
 *                       &lt;maxInclusive value="99"/>
 *                     &lt;/restriction>
 *                   &lt;/simpleType>
 *                 &lt;/attribute>
 *                 &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="symbol" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="symbol" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="number" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="forest-fire" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="unit" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="value" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="windProbability" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="temperatureProbability" type="{}unit_value" minOccurs="0"/>
 *         &lt;element name="symbolProbability" type="{}unit_value" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="stationid" type="{http://www.w3.org/2001/XMLSchema}integer" />
 *       &lt;attribute name="country" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="county" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="latitude" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="longitude" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="altitude" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "locationType", propOrder = {
    "groundCoverAndPressureAndMaximumPrecipitation"
})
public class LocationType {

    @XmlElementRefs({
        @XmlElementRef(name = "lowClouds", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "tidalwater", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "wavePeriod", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "precipitation", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "score", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "weather", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "maxWindSpeed", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "maxTemperatureNight", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "waveHeight", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "windSpeed", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "symbolProbability", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "maximumPrecipitation", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "fog", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "maxTemperatureDay", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "humidity", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "meanabsoluteerror", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "cloudiness", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "minTemperature", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "highClouds", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "groundCover", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "temperature", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "temperatureProbability", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "snowDepth", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "windProbability", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "uv", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "mediumClouds", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "surfaceTemperature", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "bias", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "minTemperatureDay", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "windDirection", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "symbol", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "stateOfTheSea", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "waveDirection", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "highestTemperature", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "lowestTemperature", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "dewpointTemperature", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "forest-fire", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "minTemperatureNight", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "windGust", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "maxWaveHeight", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "pressure", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "areaMaxWindSpeed", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "maxTemperature", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "currentDirection", type = JAXBElement.class, required = false),
        @XmlElementRef(name = "numberofobservations", type = JAXBElement.class, required = false)
    })
    protected List<JAXBElement<?>> groundCoverAndPressureAndMaximumPrecipitation;
    @XmlAttribute(name = "id")
    protected String id;
    @XmlAttribute(name = "name")
    protected String name;
    @XmlAttribute(name = "stationid")
    protected BigInteger stationid;
    @XmlAttribute(name = "country")
    protected String country;
    @XmlAttribute(name = "county")
    protected String county;
    @XmlAttribute(name = "latitude")
    protected String latitude;
    @XmlAttribute(name = "longitude")
    protected String longitude;
    @XmlAttribute(name = "altitude")
    protected String altitude;

    /**
     * Gets the value of the groundCoverAndPressureAndMaximumPrecipitation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the groundCoverAndPressureAndMaximumPrecipitation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getGroundCoverAndPressureAndMaximumPrecipitation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link JAXBElement }{@code <}{@link Cloudiness }{@code >}
     * {@link JAXBElement }{@code <}{@link Tidalwater }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link Precipitation }{@code >}
     * {@link JAXBElement }{@code <}{@link Score }{@code >}
     * {@link JAXBElement }{@code <}{@link LocationType.Weather }{@code >}
     * {@link JAXBElement }{@code <}{@link Windspeed }{@code >}
     * {@link JAXBElement }{@code <}{@link Temperature }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link Windspeed }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link Precipitation }{@code >}
     * {@link JAXBElement }{@code <}{@link Cloudiness }{@code >}
     * {@link JAXBElement }{@code <}{@link Temperature }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link Cloudiness }{@code >}
     * {@link JAXBElement }{@code <}{@link Temperature }{@code >}
     * {@link JAXBElement }{@code <}{@link Cloudiness }{@code >}
     * {@link JAXBElement }{@code <}{@link Groundcover }{@code >}
     * {@link JAXBElement }{@code <}{@link Temperature }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link LocationType.SnowDepth }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link Uv }{@code >}
     * {@link JAXBElement }{@code <}{@link Cloudiness }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link Temperature }{@code >}
     * {@link JAXBElement }{@code <}{@link LocationType.WindDirection }{@code >}
     * {@link JAXBElement }{@code <}{@link LocationType.Symbol }{@code >}
     * {@link JAXBElement }{@code <}{@link LocationType.StateOfTheSea }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link Temperature }{@code >}
     * {@link JAXBElement }{@code <}{@link Temperature }{@code >}
     * {@link JAXBElement }{@code <}{@link Temperature }{@code >}
     * {@link JAXBElement }{@code <}{@link LocationType.ForestFire }{@code >}
     * {@link JAXBElement }{@code <}{@link Temperature }{@code >}
     * {@link JAXBElement }{@code <}{@link Windspeed }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link Pressure }{@code >}
     * {@link JAXBElement }{@code <}{@link Windspeed }{@code >}
     * {@link JAXBElement }{@code <}{@link Temperature }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * {@link JAXBElement }{@code <}{@link UnitValue }{@code >}
     * 
     * 
     */
    public List<JAXBElement<?>> getGroundCoverAndPressureAndMaximumPrecipitation() {
        if (groundCoverAndPressureAndMaximumPrecipitation == null) {
            groundCoverAndPressureAndMaximumPrecipitation = new ArrayList<JAXBElement<?>>();
        }
        return this.groundCoverAndPressureAndMaximumPrecipitation;
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
     * Gets the value of the stationid property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getStationid() {
        return stationid;
    }

    /**
     * Sets the value of the stationid property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setStationid(BigInteger value) {
        this.stationid = value;
    }

    /**
     * Gets the value of the country property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCountry() {
        return country;
    }

    /**
     * Sets the value of the country property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCountry(String value) {
        this.country = value;
    }

    /**
     * Gets the value of the county property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCounty() {
        return county;
    }

    /**
     * Sets the value of the county property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCounty(String value) {
        this.county = value;
    }

    /**
     * Gets the value of the latitude property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLatitude() {
        return latitude;
    }

    /**
     * Sets the value of the latitude property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLatitude(String value) {
        this.latitude = value;
    }

    /**
     * Gets the value of the longitude property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLongitude() {
        return longitude;
    }

    /**
     * Sets the value of the longitude property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLongitude(String value) {
        this.longitude = value;
    }

    /**
     * Gets the value of the altitude property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getAltitude() {
        return altitude;
    }

    /**
     * Sets the value of the altitude property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setAltitude(String value) {
        this.altitude = value;
    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="unit" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="value" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class ForestFire {

        @XmlAttribute(name = "unit")
        protected String unit;
        @XmlAttribute(name = "value")
        protected String value;

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
         *     {@link String }
         *     
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets the value of the value property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setValue(String value) {
            this.value = value;
        }

    }


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="cm" use="required">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}decimal">
     *             &lt;minInclusive value="0"/>
     *           &lt;/restriction>
     *         &lt;/simpleType>
     *       &lt;/attribute>
     *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class SnowDepth {

        @XmlAttribute(name = "cm", required = true)
        protected BigDecimal cm;
        @XmlAttribute(name = "id")
        protected String id;

        /**
         * Gets the value of the cm property.
         * 
         * @return
         *     possible object is
         *     {@link BigDecimal }
         *     
         */
        public BigDecimal getCm() {
            return cm;
        }

        /**
         * Sets the value of the cm property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigDecimal }
         *     
         */
        public void setCm(BigDecimal value) {
            this.cm = value;
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


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="number" use="required">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
     *             &lt;minInclusive value="0"/>
     *             &lt;maxInclusive value="9"/>
     *           &lt;/restriction>
     *         &lt;/simpleType>
     *       &lt;/attribute>
     *       &lt;attribute name="meter" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class StateOfTheSea {

        @XmlAttribute(name = "number", required = true)
        protected int number;
        @XmlAttribute(name = "meter")
        protected String meter;
        @XmlAttribute(name = "name")
        protected String name;
        @XmlAttribute(name = "id")
        protected String id;

        /**
         * Gets the value of the number property.
         * 
         */
        public int getNumber() {
            return number;
        }

        /**
         * Sets the value of the number property.
         * 
         */
        public void setNumber(int value) {
            this.number = value;
        }

        /**
         * Gets the value of the meter property.
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        public String getMeter() {
            return meter;
        }

        /**
         * Sets the value of the meter property.
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        public void setMeter(String value) {
            this.meter = value;
        }

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


    /**
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="name" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="number" use="required" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Symbol {

        @XmlAttribute(name = "id")
        protected String id;
        @XmlAttribute(name = "name")
        protected String name;
        @XmlAttribute(name = "number", required = true)
        protected BigInteger number;

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
         * Gets the value of the number property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getNumber() {
            return number;
        }

        /**
         * Sets the value of the number property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setNumber(BigInteger value) {
            this.number = value;
        }

    }


    /**
     * Element denoting a weather symbol by name or number.
     * 						
     * 
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="name">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *           &lt;/restriction>
     *         &lt;/simpleType>
     *       &lt;/attribute>
     *       &lt;attribute name="number" use="required">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}integer">
     *             &lt;minInclusive value="0"/>
     *             &lt;maxInclusive value="99"/>
     *           &lt;/restriction>
     *         &lt;/simpleType>
     *       &lt;/attribute>
     *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="symbol" type="{http://www.w3.org/2001/XMLSchema}integer" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Weather {

        @XmlAttribute(name = "name")
        protected String name;
        @XmlAttribute(name = "number", required = true)
        protected int number;
        @XmlAttribute(name = "id")
        protected String id;
        @XmlAttribute(name = "symbol")
        protected BigInteger symbol;

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
         * Gets the value of the number property.
         * 
         */
        public int getNumber() {
            return number;
        }

        /**
         * Sets the value of the number property.
         * 
         */
        public void setNumber(int value) {
            this.number = value;
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

        /**
         * Gets the value of the symbol property.
         * 
         * @return
         *     possible object is
         *     {@link BigInteger }
         *     
         */
        public BigInteger getSymbol() {
            return symbol;
        }

        /**
         * Sets the value of the symbol property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigInteger }
         *     
         */
        public void setSymbol(BigInteger value) {
            this.symbol = value;
        }

    }


    /**
     * Element denoting the wind direction by angle or
     * 						compass direction.
     * 						
     * 
     * <p>Java class for anonymous complex type.
     * 
     * <p>The following schema fragment specifies the expected content contained within this class.
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="deg" use="required">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}decimal">
     *             &lt;minInclusive value="0"/>
     *             &lt;maxInclusive value="990"/>
     *           &lt;/restriction>
     *         &lt;/simpleType>
     *       &lt;/attribute>
     *       &lt;attribute name="name">
     *         &lt;simpleType>
     *           &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
     *           &lt;/restriction>
     *         &lt;/simpleType>
     *       &lt;/attribute>
     *       &lt;attribute name="id" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class WindDirection {

        @XmlAttribute(name = "deg", required = true)
        protected BigDecimal deg;
        @XmlAttribute(name = "name")
        protected String name;
        @XmlAttribute(name = "id")
        protected String id;

        /**
         * Gets the value of the deg property.
         * 
         * @return
         *     possible object is
         *     {@link BigDecimal }
         *     
         */
        public BigDecimal getDeg() {
            return deg;
        }

        /**
         * Sets the value of the deg property.
         * 
         * @param value
         *     allowed object is
         *     {@link BigDecimal }
         *     
         */
        public void setDeg(BigDecimal value) {
            this.deg = value;
        }

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

}
