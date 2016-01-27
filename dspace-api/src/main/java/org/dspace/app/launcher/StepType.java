
package org.dspace.app.launcher;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.*;


/**
 * <p>Java class for stepType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="stepType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="className" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="argument" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="passuserargs" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "stepType", propOrder = {
    "className",
    "argument"
})
public class StepType {
    @XmlElement(name="class")
    protected String className;
    protected List<String> argument;
    @XmlAttribute(name = "passuserargs")
    protected String passuserargs;

    /**
     * Gets the value of the className property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getClassName() {
        return className;
    }

    /**
     * Sets the value of the className property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */

    public void setClassName(String value) {
        this.className = value;
    }

    /**
     * Gets the value of the argument property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the argument property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getArgument().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getArgument() {
        if (argument == null) {
            argument = new ArrayList<String>();
        }
        return this.argument;
    }

    /**
     * Gets the value of the passuserargs property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPassuserargs() {
        return passuserargs;
    }

    /**
     * Sets the value of the passuserargs property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPassuserargs(String value) {
        this.passuserargs = value;
    }

}
