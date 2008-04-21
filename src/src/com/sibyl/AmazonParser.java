package com.sibyl;


import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Parse amazon xml answer when searching for cover
 * 
 * @author sibyl
 *
 */
public class AmazonParser extends DefaultHandler{

    private StringBuffer buffer;
    private static final String TAG = "MediumImage";
    private static final String TAG_CONTENT = "URL";

    // true when we can read data
    private boolean read;
    // true when we have found the TAG 
    private boolean found;
    // false when we have read first data
    private boolean first;

    public void startDocument() throws SAXException {
        // init values
        first = true;
        read = false;
        found = false;
        buffer = new StringBuffer();
    }
    
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException{
        if(first && qName.equals(TAG)){
            found = true;
        }else if( first && found && qName.equals(TAG_CONTENT)){
            read = true;
        }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException{
        if(found && qName.equals(TAG)){
            found = false;
            first = false;
        }else if( found && qName.equals(TAG_CONTENT)){
            read = false;
        }
    }

    public void characters(char[] ch,int start, int length) throws SAXException{
        if(read){
            String lecture = new String(ch,start,length);
            buffer.append(lecture);  
            read = false;
        }
    }

    /**
     * get result (cover url)
     * 
     * @return null if nothing found
     */
    public String getResult(){
        return buffer.toString();
    }

}