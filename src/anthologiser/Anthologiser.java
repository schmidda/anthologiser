/*
 * This file is part of Anthologiser.
 * Anthologiser is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Anthologiser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Anthologiser.  If not, see <http://www.gnu.org/licenses/>.
 */
package anthologiser;
import java.io.File;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import java.io.StringReader;
import java.io.FileInputStream;
import org.xml.sax.InputSource;
import java.util.HashMap;
import java.util.ArrayList;
import hritserver.json.JSONDocument;

/**
 * A commandline tool to split an anthology into poems AND create an 
 * anthology file and a record of its versions. Put the poems into a 
 * directory structure that can be merged with other anthologies, so 
 * building up the data structure for automatic uploading.
 * directory structure will be:
 * ([...] is a folder, {...} is a file) 
 * [specified-folder-name]
 *           |
 *           -------------{versions.json} 
 *           |
 *           -------------[anthologies]
 *           |                  |
 *           |                  ------{anthology-1}...
 *           |
 *           -------------subdir1...
 *                           |
 *                        [work-1]...
 *                           |
 *                           ----{version-text-1}...
 * The versions file in JSON format is used to provide parameters for the 
 * upload - descriptions of each version. These are extracted from the 
 * anthology files
 * @author desmond 15/5/2012
 */
public class Anthologiser 
{
    /** keys for config file */
    static String MERGES = "merges";
    static String NAME = "name";
    static String VERSIONS = "versions";
    static String REMOVALS = "removals";
    static int MAX_POEMS_PER_FOLDER = 10;
    /** the source anthology file */
    File src;
    /** updatable record of groups and versions */
    VersionsDocument versions;
    /** collection of links and title */
    Anthology anthology;
    /** base URL for links */
    String linkBase;
    /** folder to store everything in*/
    File folder;
    /** folder to store anthologies in */
    File anthologiesDir;
    /** config file */
    JSONDocument config;
    /** normalised set of poem names */
    HashMap<String,String> poemNames;
    /** map of MVD-names to MVD-folders */
    FileMap poems;
    /** regular expression for matching words to remove from poem names */
    String removals;
    static final String DEFAULT_MAIN_FOLDER = "poems";
    static final String ANTHOLOGIES_FOLDER = "anthologies";
    /** 
     * Initialise everything
     */
    public Anthologiser()
    {
        linkBase = "/";
    }
    /**
     * Tell the user how to use this application
     */
    private static void usage()
    {
        System.out.println( "usage: java -jar Anthologiser.jar "
            +"[-f folder] [-l link-base] [-c config] file" );
    }
    /**
     * Load the translations of raw to normalised poem names
     * @return a map of actual poem names to their normalised equivalents
     */
    private HashMap<String,String> loadPoemNames()
    {
        poemNames = new HashMap<String,String>();
        ArrayList list = (ArrayList)config.get( MERGES );
        for ( int i=0;i<list.size();i++ )
        {
            JSONDocument subDoc = (JSONDocument)list.get(i);
            ArrayList aliases = (ArrayList)subDoc.get(VERSIONS);
            for ( int j=0;j<aliases.size();j++ )
            {
                poemNames.put( (String)aliases.get(j), 
                    (String)subDoc.get(NAME) );
            }
        }
        // load removals
        ArrayList matchList = (ArrayList)config.get( REMOVALS );
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<matchList.size();i++ )
        {
            String item = (String)matchList.get( i );
            if ( sb.length()==0 )
                sb.append( item );
            else
            {
                sb.append( "|" );
                sb.append( item );
            }
        }
        removals = sb.toString();
        removals = removals.replace("[","\\[");
        removals = removals.replace("]","\\]");
        return poemNames;
    }
    /**
     * Check the commandline arguments for sanity
     * @param args the arguments
     * @return true if they are usable, else false
     */
    public boolean checkArgs( String[] args ) throws Exception
    {
        boolean sane = true;
        if ( args.length >= 1 )
        {
            File configFile = null;
            for ( int i=0;i<args.length-2;i++ )
            {
                if ( args[i].length()==2&&args[i].charAt(0)=='-' )
                {
                    switch ( args[i].charAt(1) )
                    {
                        case 'l':   // link
                            if ( args.length < i+3 )
                                sane = false;
                            else
                                linkBase = args[i+1];
                            break;
                        case 'f':   // folder
                            if ( args.length < i+3 )
                                sane = false;
                            else
                            {
                                folder = new File(args[i+1]);
                                if ( !folder.exists() )
                                    folder.mkdirs();
                            }
                            break;
                        case 'c':   // config file
                            configFile = new File( args[i+1] );
                            break;
                    }
                }
            }
            File file = new File( args[args.length-1] );
            if ( !file.isFile() || !file.getName().endsWith(".xml") )
            {
                System.out.println( file.getName()+" is not a file or not XML" );
                sane = false;
            }
            else
                src = new File( args[args.length-1] );
            // ensure default folder is created/specified
            if ( folder == null )
            {
                folder = new File( DEFAULT_MAIN_FOLDER );
                if ( !folder.exists() )
                    folder.mkdir();
            }
            // ensure at least an empty config file
            if ( configFile == null )
                configFile = new File( "config.json" );
            if ( configFile.exists() )
                config = JSONDocument.internalise( configFile, "UTF-8" );
            else
                config = new JSONDocument();
            poemNames = loadPoemNames();
            // ensure anthologies folder exists
            anthologiesDir = new File( folder, "%"+ANTHOLOGIES_FOLDER );
            // normalise linkBase
            if ( !linkBase.endsWith("/") )
                linkBase += "/";
        }
        else
        {
            sane = false;
        }
        return sane;
    }
    /**
     * Look one level down for a div element with a description
     * @param elem the top-level element to start from
     * @return the description or more often null
     */
    String extractDescription( Node elem )
    {
        String desc = null;
        Node child = elem.getFirstChild();
        while ( child != null )
        {
            if ( child.getNodeType()==Node.ELEMENT_NODE )
            {
                Element e = (Element) child;
                if ( e.getNodeName().equals("div") 
                    && e.hasAttribute("type")
                    &&e.getAttribute("type").equals("source") )
                {
                    String text = child.getTextContent();
                    if ( text != null )
                    {
                        desc = text;
                        int index = desc.lastIndexOf(",");
                        if ( index != -1 )
                            desc = desc.substring( 0, index );
                        desc = desc.trim();
                        break;
                    }
                }
            }
            child = child.getNextSibling();
        }
        return desc;
    }
    /**
     * Normalise a poem's name by removing unwanted junk and equating
     * names for the same poem using the config file
     * @param raw the raw poem name from the src
     * @return a normalised poem name
     */
    private String normaliseName( String raw )
    {
        String answer;
        if ( poemNames.containsKey(raw) )
            answer = poemNames.get(raw);
        else
            answer = raw;
        answer = answer.replaceAll(":|\\?|\"","");
        return answer.replaceAll(removals, "").trim();
    }
    /**
     * Convert an element and all its content to a byte array
     * @param root the root element
     * @return 
     */
    byte[] toBytes( Element root ) throws Exception
    {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        printOne( bos, root );
        return bos.toByteArray();
    }
    /**
     * Add a poem to the poems hashmap. There may be several poems of the 
     * same name
     * @param poem the normalised name or title of the poem
     * @param file the name for the file
     * @param root the document's root element
     */
    void addPoem( String poem, String file, Element root ) 
        throws Exception
    {
        String key = "%"+poem;
        FolderItem fi;
        if ( poems.containsKey(key) )
            fi = poems.get( key );
        else
        {
            fi = new FolderItem();
            poems.put( new PunctIgnoreString(key), fi );
        }
        String fname = Utils.fileName( file );
        String suffix = Utils.fileSuffix( file );
        fi.add( poems.getTempDir(), toBytes(root), "", fname, suffix );
    }
    /**
     * Split the document rooted at elem into individual poems prefixed 
     * by comments starting with "***"
     * @param doc the parent document
     * @param elem the element below which poems exist
     */
    private void split( Document doc, Element elem ) throws Exception
    {
        Node child = elem.getFirstChild();
        Element tei = null;
        Element text = null;
        String title = null;
        while ( child != null )
        {
            if ( child.getNodeType()==Node.COMMENT_NODE 
                && child.getTextContent().trim().startsWith("***") )
            {
                if ( tei != null&&title!=null )
                    addPoem(title,src.getName(),tei);
                tei = doc.createElement("TEI");
                Element body = doc.createElement("body");
                tei.appendChild( body );
                text = doc.createElement("text");
                body.appendChild( text );
                String content = child.getTextContent().substring(4).trim();
                content = content.replace("/","_");
                title = normaliseName( content );
            }
            else if ( text != null )
            {
                Node clone = child.cloneNode(true);
                if ( clone.getNodeType()==Node.ELEMENT_NODE
                    && clone.getNodeName().equals("div") )
                {
                    String desc = extractDescription( clone );
                    if ( desc != null )
                    {
                        String sName = simpleName(src.getName());
                        if ( !anthology.descriptionSet() )
                            anthology.setDescription( desc );
                        if ( !anthology.titleSet() )
                            anthology.setTitle( sName );
                        if ( !versions.containsKey(sName) )
                            versions.addVersion( sName, desc );
                    }
                }
                text.appendChild( clone );
            }
            else if ( child.getNodeType()==Node.ELEMENT_NODE )
            {
                Element e = (Element) child;
                String name = e.getNodeName();
                if ( name.equals("body")||name.equals("text") )
                {
                    child = e.getFirstChild();
                    continue;
                }
            }
            child = child.getNextSibling();
        }
        if ( tei != null&&title!=null )
             addPoem(title,src.getName(),tei);   
    }
    /**
     * Write an element and its children to disk as an XML document
     * @param fos the file output stream to write to
     * @param elem the element to print
     */
    void printElementBody( OutputStream fos, Element elem )
        throws Exception
    {

        // recurse into its children
        Node child = elem.getFirstChild();
        while ( child != null )
        {
            printOne( fos, child );
            child = child.getNextSibling();
        }
    }
    /**
     * Print an element's end-code
     * @param fos the stream to write to
     * @param elem the element in question
     */
    void printElementEnd( OutputStream fos, Element elem )
        throws Exception
    {
        if ( elem.getFirstChild()!=null )
        {
            fos.write( '<' );
            fos.write('/');
            fos.write(elem.getNodeName().getBytes("UTF-8") );
            fos.write( '>' );
        }
    }
    /**
     * Print a single element node
     * @param fos the stream to write to
     * @param elem the element in question
     */
    void printElementStart( OutputStream fos, Element elem ) 
        throws Exception
    {
        fos.write('<');
        fos.write( elem.getNodeName().getBytes("UTF-8") );
        NamedNodeMap attrs = elem.getAttributes();
        for ( int j=0;j<attrs.getLength();j++ )
        {
            Node attr = attrs.item( j );
            fos.write( ' ' );
            fos.write( attr.getNodeName().getBytes("UTF-8") );
            fos.write( "=\"".getBytes() );
            fos.write( attr.getNodeValue().getBytes("UTF-8") );
            fos.write( '"' );
        }
        if ( elem.getFirstChild()==null )
        {
            fos.write('/');
            fos.write('>');
        }
        else
            fos.write('>');
    }
    /**
     * Write all the versions to the string builders in the map
     * @param fos the data stream to write to
     * @param node the node to write out
     */
    void printOne( OutputStream fos, Node node ) throws Exception
    {
        if ( node.getNodeType()==Node.TEXT_NODE )
        {
            String content = node.getTextContent();
            content = content.replace( "&","&amp;" );
            fos.write( content.getBytes("UTF-8") );
        }
        else if ( node.getNodeType()==Node.ELEMENT_NODE )
        {
            printElementStart( fos, (Element)node );
            printElementBody( fos, (Element)node );
            printElementEnd( fos, (Element)node );
        }
        else if ( node.getNodeType()==Node.COMMENT_NODE )
        {
            fos.write("<!--".getBytes("UTF-8") );
            fos.write(node.getTextContent().getBytes("UTF-8") );
            fos.write("-->".getBytes("UTF-8") );
        }
    }
    /** 
     * simplify the source file name by removing the extension
     * @param fileName the name to simplify
     * @return the shortened name
     */
    private String simpleName( String fileName )
    {
        String fName = fileName;
        int index = fileName.lastIndexOf(".");
        if ( index != -1 )
            fName = fileName.substring(0,index);
        return fName;
    }
    /**
     * Parse the input file
     */
    public void parse() throws Exception
    {
        try
        {
            FileInputStream fis = new FileInputStream( src );
            byte[] data = new byte[(int)src.length()];
            fis.read( data );
            fis.close();
            anthology = new Anthology( anthologiesDir, 
                simpleName(src.getName()), linkBase );
            versions = new VersionsDocument( anthologiesDir.getParentFile() );
            versions.internalise();
            String tei = new String( data, "UTF-8" );
            poems = new FileMap( folder );
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            StringReader sr = new StringReader( tei );
            InputSource is = new InputSource( sr );
            Document doc = db.parse( is ); 
            Element root = doc.getDocumentElement();
            split( doc, root );
            poems.save( folder, anthology );
            anthology.externalise();
            versions.externalise();
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
        }
    }
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) 
    {
        Anthologiser a = new Anthologiser();
        try
        {
            if ( a.checkArgs(args) )
            {
                a.parse();
            }  
            else
                usage();
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
        }
    }
}
