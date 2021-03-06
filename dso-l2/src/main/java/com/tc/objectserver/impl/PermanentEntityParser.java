/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.objectserver.impl;

import com.tc.entity.VoltronEntityMessage;
import com.tc.object.EntityID;
import com.tc.objectserver.entity.CreateSystemEntityMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.terracotta.config.Entities;
import org.terracotta.config.Entity;
import org.terracotta.config.TcConfig;
import org.w3c.dom.Element;

/**
 * This parser is intended to figure out which entities are to be created for the cluster 
 * through server configuration.  
 * 
 * Parsing involves iterating the proper XML tag and supplying the type, name and version 
 * of the entity to be produced.  Configuration of these entities is slightly different from 
 * client created entities.  Because these entities are configured through XML, what can be passed as 
 * raw bytes of configuration to entity creation is limited to two choices.  If the {@code <properties/>} tag
 * is used, each tag contained will be turned into a key value pair in a properties object.  The properties 
 * object will be serialized into raw bytes using {@code Properties.store() } and passed to the 
 * Entity service for construction.  Otherwise, the raw xml bytes will be passed to the Entity service
 * for processing there.
 */
public class PermanentEntityParser {
  public static List<VoltronEntityMessage> parseEntities(TcConfig config) {
      Entities e = config.getEntities();
      List<VoltronEntityMessage> msgs = null;
      if (e != null) {
        msgs = new ArrayList<>(e.getEntity().size());
        for (Entity b : e.getEntity()) {
          String name = b.getName();
          String type = b.getType();
          int version = b.getVersion();
          Entity.Configuration c = b.getConfiguration();
          byte[] data;
          if (c != null) {
            Entity.Configuration.Properties m = c.getProperties();
            if (m != null) {
              Properties prop = new Properties();
              List<Element> list = m.getAny();
              for (Element pe : list) {
                prop.setProperty(pe.getTagName(), pe.getTextContent());
              }
              try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                prop.store(bos, null);
                data = bos.toByteArray();
              } catch (IOException ioe) {
                data = new byte[0];
              }
            } else {
              Element any = c.getAny();
              if (any != null) {
                try {
                  TransformerFactory transFactory = TransformerFactory.newInstance();
                  Transformer transformer = transFactory.newTransformer();
                  StringWriter buffer = new StringWriter();
                  transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                  transformer.transform(new DOMSource(any), new StreamResult(buffer));
                  String str = buffer.toString();
                  data = str.getBytes();
                } catch (TransformerException te) {
                  data = new byte[0];
                }
              } else {
                data = new byte[0];
              }
            }
          } else {
            data = new byte[0];
          }
          msgs.add(createMessage(type, name, version, data));
        }
      } else {
        msgs = Collections.emptyList();
      }
      return msgs;
  }
  
  public static VoltronEntityMessage createMessage(String type, String name, int version, byte[] data) {
    return new CreateSystemEntityMessage(new EntityID(type, name),version, data);
  }
}
