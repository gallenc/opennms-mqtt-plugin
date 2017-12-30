package org.opennms.plugins.mqtt.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.opennms.protocols.xml.config.XmlRrd;

@XmlRootElement(name="json-parser")
@XmlAccessorType(XmlAccessType.NONE)
public class JsonParserConfig {

    /** The XML RRAs list. */
    @XmlElement(name="rra")
    private List<String> m_xmlRras = new ArrayList<String>();
    
    /**
     * Gets the XML RRAs.
     *
     * @return the XML RRAs
     */
    public List<String> getXmlRras() {
        return m_xmlRras;
    }

    /**
     * Sets the XML RRAs.
     *
     * @param xmlRras the new XML RRAs
     */
    public void setXmlRras(List<String> xmlRras) {
        m_xmlRras = xmlRras;
    }


}
