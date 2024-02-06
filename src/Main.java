import javax.naming.*;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.directory.*;
import javax.xml.xpath.*;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class Main {
    public static void main(String[] args) {
        try {
            Properties env = new Properties();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, "ldap://localhost:10389");
            env.put(Context.SECURITY_PRINCIPAL, "uid=admin,ou=system");
            env.put(Context.SECURITY_CREDENTIALS, "secret");

            DirContext context = new InitialDirContext(env);

            createOrganizationalUnit("ou=Grupos,dc=example,dc=com", context);
            createOrganizationalUnit("ou=Usuarios,dc=example,dc=com", context);

            processXmlFile("public/files/AddGrupo1.xml", context);
            processXmlFile("public/files/AddGrupo2.xml", context);
            processXmlFile("public/files/AddGrupo3.xml", context);
            processXmlFile("public/files/AddUsuario1.xml", context);
            processXmlFile("public/files/ModifyUsuario.xml", context);

            context.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void createOrganizationalUnit(String ouDN, DirContext context) {
        try {
            context.getAttributes(ouDN);
            System.out.println("A unidade organizacional já existe: " + ouDN);
        } catch (NamingException e) {
            try {
                Attributes ouAttributes = new BasicAttributes();
                ouAttributes.put("objectClass", "organizationalUnit");
                context.createSubcontext(ouDN, ouAttributes);
                System.out.println("Unidade organizacional criada com sucesso: " + ouDN);
            } catch (NamingException ne) {
                System.err.println("Erro ao criar unidade organizacional: " + ne.getMessage());
            }
        }
    }

    private static void processXmlFile(String xmlFileName, DirContext context) throws Exception {
        Document doc = parseXmlFile(xmlFileName);
        XPath xpath = XPathFactory.newInstance().newXPath();

        String className = xpath.evaluate("/add/@class-name", doc);
        if ("Grupo".equals(className)) {
            processGrupo(doc, context, xpath);
        } else if ("Usuario".equals(className)) {
            processUsuario(doc, context, xpath);
        } else if ("modify".equals(className.toLowerCase())) {
            modifyUsuario(doc, context, xpath);
        }
    }

    private static void processGrupo(Document doc, DirContext context, XPath xpath) throws Exception {
        String identifier = xpath.evaluate("/add/add-attr[@attr-name='Identificador']/value/text()", doc);
        String description = xpath.evaluate("/add/add-attr[@attr-name='Descricao']/value/text()", doc);

        String entryDN = "cn=" + identifier + ",ou=Grupos,dc=example,dc=com";
        Attributes attributes = new BasicAttributes();
        Attribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("top");
        objectClass.add("groupOfNames");
        attributes.put(objectClass);

        attributes.put("cn", identifier);
        attributes.put("description", description);

        attributes.put(new BasicAttribute("member", "uid=example,ou=Usuarios,dc=example,dc=com"));

        createEntry(context, entryDN, attributes);
    }

    private static void processUsuario(Document doc, DirContext context, XPath xpath) throws Exception {
        String fullName = xpath.evaluate("/add/add-attr[@attr-name='Nome Completo']/value/text()", doc);
        String login = xpath.evaluate("/add/add-attr[@attr-name='Login']/value/text()", doc);
        String phone = xpath.evaluate("/add/add-attr[@attr-name='Telefone']/value/text()", doc);

        if (!isValidPhoneNumber(phone)) {
            System.out.println("Número de telefone inválido: " + phone);
            return;
        }

        String entryDN = "uid=" + login + ",ou=Usuarios,dc=example,dc=com";
        Attributes attributes = new BasicAttributes();
        Attribute objectClass = new BasicAttribute("objectClass");
        objectClass.add("top");
        objectClass.add("person");
        objectClass.add("organizationalPerson");
        objectClass.add("inetOrgPerson");
        attributes.put(objectClass);

        attributes.put("uid", login);
        attributes.put("cn", fullName);
        attributes.put("sn", fullName.substring(fullName.lastIndexOf(' ') + 1));
        attributes.put("telephoneNumber", phone);

        createEntry(context, entryDN, attributes);
    }

    private static void modifyUsuario(Document doc, DirContext context, XPath xpath) throws Exception {
        String login = xpath.evaluate("/modify/association[@state='associated']/text()", doc);
        String userDN = "uid=" + login + ",ou=Usuarios,dc=example,dc=com";

        List<ModificationItem> modItems = new ArrayList<>();

        NodeList addValues = (NodeList) xpath.evaluate("/modify/modify-attr[@attr-name='Grupo']/add-value/value/text()", doc, XPathConstants.NODESET);
        for (int i = 0; i < addValues.getLength(); i++) {
            String value = addValues.item(i).getNodeValue();
            String groupDN = "cn=" + value + ",ou=Grupos,dc=example,dc=com";
            ModificationItem item = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("member", groupDN));
            modItems.add(item);
        }

        NodeList removeValues = (NodeList) xpath.evaluate("/modify/modify-attr[@attr-name='Grupo']/remove-value/value/text()", doc, XPathConstants.NODESET);
        for (int i = 0; i < removeValues.getLength(); i++) {
            String value = removeValues.item(i).getNodeValue();
            String groupDN = "cn=" + value + ",ou=Grupos,dc=example,dc=com";
            ModificationItem item = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("member", groupDN));
            modItems.add(item);
        }

        if (!modItems.isEmpty()) {
            context.modifyAttributes(userDN, modItems.toArray(new ModificationItem[0]));
        }
    }

    private static boolean isValidPhoneNumber(String phone) {
        String digits = phone.replaceAll("[^\\d]", "");
        return !digits.isEmpty() && digits.length() >= 10;
    }

    private static Document parseXmlFile(String xmlFileName) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(xmlFileName);
    }

    private static void createEntry(DirContext context, String dn, Attributes attributes) {
        try {
            context.createSubcontext(dn, attributes);
            System.out.println("Entrada criada com sucesso: " + dn);
        } catch (NameAlreadyBoundException e) {
            System.out.println("A entrada já existe: " + dn);
        } catch (NamingException e) {
            System.err.println("Erro ao criar entrada: " + dn + ", " + e.getMessage());
        }
    }
}