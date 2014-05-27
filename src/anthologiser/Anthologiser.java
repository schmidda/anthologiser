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
 * 1. to do: convert floatingText to div - done but not tested
 * 2. Make sub-directories for Harpur more fixed, less fluid.
 */
package anthologiser;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.FileOutputStream;
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
import java.util.Set;
import java.util.Iterator;
//import javax.xml.transform.Transformer;
//import javax.xml.transform.TransformerFactory;
//import javax.xml.transform.dom.DOMSource;
//import javax.xml.transform.stream.StreamResult;

/**
 * <p>A commandline tool to split an anthology into poems AND create an 
 * anthology file and a record of its versions. Put the poems into a 
 * directory structure that can be merged with other anthologies, so 
 * building up the data structure for automatic uploading.
 * directory structure will be:</p>
 * <pre>([...] is a folder, {...} is a file) 
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
 *                           ----{version-text-1}...</pre>
 * <p>The versions file in JSON format is used to provide parameters for the 
 * upload - descriptions of each version. These are extracted from the 
 * anthology files</p>
 * @author desmond 15/5/2012
 */
public class Anthologiser 
{
    static String MISC_SERVER = "http://dev.austese.net/harpur/";
    static String MISC_FOLDER = "@misc";
    /** keys for config file */
    static int MAX_POEMS_PER_FOLDER = 15;
    /** the source anthology file */
    File src;
    /** updatable record of groups and versions */
    VersionsDocument versions;
    /** collection of links and title */
    HashMap<String,Anthology> anthologies;
    /** base URL for links */
    String linkBase;
    /** folder to store everything in*/
    File folder;
    /** folder at top level */
    File topLevelFolder;
    /** folder to store anthologies in */
    File miscDir;
    /** archive settings: base_url etc. */
    String archive;
    /** map of MVD-names to MVD-folders */
    FileMap poems;
//    /** configuration settings */
//    Config config;
    HashMap<String,String> works;
    /** if true join the various anthologies into an index */
    boolean join;
    /** true if using subfolders starting with space */
    boolean useSubFolders;
    static final String DEFAULT_MAIN_FOLDER = "poems";
    static final String ARCHIVE_STR = 
        "{\n    \"base_url\": \"http://localhost:8080/\"\n}\n";
    /** 
     * Initialise everything
     */
    public Anthologiser()
    {
        linkBase = "/";
        archive = ARCHIVE_STR;
        anthologies = new HashMap<String,Anthology>();
        works = new HashMap<String,String>(800);
    }
    /**
     * Tell the user how to use this application
     */
    private static void usage()
    {
        System.out.println( "usage: java -jar Anthologiser.jar "
            +"[-f folder] [-l link-base] [-c config] [-w works] file" );
    }
    /**
     * Read an optional h-numbers file, identifiers for works
     * @param file
     * @throws Exception 
     */
    private void readWorks( String file ) throws Exception
    {
        try
        {
            File f = new File( file );
            if ( f.exists() )
            {
                int len = (int)f.length();
                char[] buf = new char[len];
                FileReader fr = new FileReader( f );
                fr.read( buf );
                String text = new String( buf );
                String[] lines = text.split("\n");
                for ( int i=0;i<lines.length;i++ )
                {
                    String[] parts = lines[i].split("\t");
                    if ( parts.length>=2 && parts[0].length()>0 
                        && parts[1].length()>0 )
                        works.put( parts[0], parts[1] );
                }
            }
        }
        catch ( Exception e )
        {
            throw new Exception( "failed to read file "+file );
        }
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
//            File configFile = null;
            for ( int i=0;i<args.length-2;i++ )
            {
                if ( args[i].length()==2&&args[i].charAt(0)=='-' )
                {
                    switch ( args[i].charAt(1) )
                    {
                        case 'j':
                            join = true;
                            break;
                        case 'l':   // link
                            if ( args.length < i+3 )
                                sane = false;
                            else
                                linkBase = args[i+1];
                            break;
                        case 'w':    // h-numbers CSV (tab-delimited) file
                            readWorks( args[i+1] );
                            break;
                        case 'f':   // folder
                            if ( args.length < i+3 )
                                sane = false;
                            else
                            {
                                folder = new File(args[i+1]);
                                if ( !folder.exists() )
                                    folder.mkdirs();
                                String[] parts = args[i+1].split("/");
                                if ( parts.length > 0 )
                                    topLevelFolder = new File(parts[0]);
                            }
                            break;
                        case 's':   // use sub folders
                            useSubFolders = true;
                            break;
                    }
                }
            }
            if ( topLevelFolder == null )
                topLevelFolder = folder;
            File file = new File( args[args.length-1] );
            if ( !file.isFile() || !file.getName().endsWith(".xml") )
            {
                System.out.println( file.getName()+" is not a file or not XML" );
                sane = false;
            }
            else
                src = file;
            // ensure default folder is created/specified
            if ( folder == null )
            {
                folder = new File( DEFAULT_MAIN_FOLDER );
                if ( !folder.exists() )
                    folder.mkdir();
            }
            // ensure anthologies folder exists
            miscDir = new File( topLevelFolder, MISC_FOLDER );
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
     * Convert an element and all its content to a byte array
     * @param root the root element
     * @return a byte array
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
     * @param title the normalised name or title of the poem
     * @param file the name for the file
     * @param hWork the key for the work name in works
     * @param hVersion the individual version ID
     * @param root the document's root element
     */
    void addPoem( String title, String file, String hWork, String hVersion, 
            Element root ) throws Exception
    {
        String key = "%"+hWork;
        MultiFormatDir mfd;
        if ( poems.containsKey(key) )
            mfd = poems.get( key );
        else
        {
            File mfdDir = new File( poems.getTempDir(), "%"+hWork );
            mfd = new MultiFormatDir( mfdDir );
            title = Titeliser.getTitle( title );
            mfd.addConfigPair( JSONKeys.TITLE, title );
            poems.put( key, mfd );
        }
        String fname = Utils.fileName( file );
        String suffix = Utils.fileSuffix( file );
        mfd.add( toBytes(root), "", fname, hVersion, suffix );
    }
    private static int push( StringBuilder sb, char token, int state )
    {
        sb.append( token );
        return state;
    }
    private static int pop( StringBuilder saved, StringBuilder dst, int state )
    {
        dst.append( saved );
        saved.setLength( 0 );
        return state;
    }
//    private static String removePageNos( String src )
//    {
//        StringBuilder sb = new StringBuilder();
//        int state = 0;
//        StringBuilder saved = new StringBuilder();
//        for ( int i=0;i<src.length();i++ )
//        {
//            char token = src.charAt(i);
//            switch ( state )
//            {
//                case 0:
//                    if ( token == '(' )
//                        state = push(saved,token,1);
//                    else if ( token == '[' )
//                        state = push(saved,token,2);
//                    else if ( token != ' ' || sb.charAt(sb.length()-1) != ' ')
//                        sb.append( token );
//                    break;
//                case 1: // look for 'p'
//                    if ( token == 'p' )
//                        state = push(saved,token,3);
//                    else
//                    {
//                        state = pop(saved,sb,0);
//                        sb.append(token);
//                    }
//                    break;
//                case 2: // look for 'p'
//                    if ( token == 'p' )
//                        state = push(saved,token,4);
//                    else
//                        state = pop(saved,sb,0);
//                    break;
//                case 3: // look for '.' after '(p'
//                    if ( token=='.' )
//                        state = push(saved,token,5);
//                    else
//                        state = pop(saved,sb,0);
//                    break;
//                case 4: // look for '.' after '[p'
//                    if ( token=='.' )
//                        state = push(saved,token,6);
//                    else
//                        state = pop(saved,sb,0);
//                    break;
//                case 5: // look for digits
//                    if ( Character.isDigit(token)
//                        ||Character.isWhitespace(token) )
//                        state = push(saved,token,5);
//                    else if ( token==')' )
//                    {
//                        saved.setLength(0);
//                        state = 0;
//                    }
//                    else
//                        state = pop(saved,sb,0);
//                    break;
//                case 6:
//                    if ( Character.isDigit(token)
//                        ||Character.isWhitespace(token) )
//                        state = push(saved,token,6);
//                    else if ( token==']' )
//                    {
//                        saved.setLength(0);
//                        state = 0;
//                    }
//                    else
//                        state = pop(saved,sb,0);
//                    break;
//            }
//        }
//        if ( saved.length() > 0 )
//            sb.append( saved.toString());
//        return sb.toString();
//    }
    private String hGetWork( String hVersion )
    {
        if ( hVersion.length() > 1 )
        {
            StringBuilder sb = new StringBuilder(hVersion);
            char token = sb.charAt(sb.length()-1);
            while ( sb.length()> 1 && Character.isLowerCase(token) )
            {
                sb.setLength(sb.length()-1);
                token = sb.charAt(sb.length()-1);
            }
            return sb.toString();
        }
        else
            return hVersion;
    }
    /**
     * Merge all the anthologies into a single file
     */
    void joinAnthologies() throws Exception
    {
        Set<String> keys = anthologies.keySet();
        Iterator<String> iter = keys.iterator();
        String key1 = iter.next();
        if ( key1 != null )
        {
            Anthology anth = anthologies.get(key1);
            File anthologiesDir = anth.getAnthologiesDir();
            File dst = new File( anthologiesDir, "index" );
            if ( !dst.exists() )
            {
                File[] files = anthologiesDir.listFiles();
                dst.createNewFile();
                FileOutputStream fos = new FileOutputStream(dst);
                byte[] bytes = 
                    "<div id=\"listContainer\">\n<ul id=\"expList\">".getBytes();
                fos.write( bytes );
                for ( int i=0;i<files.length;i++ )
                {
                    FileInputStream fis = new FileInputStream(files[i]);
                    byte[] data = new byte[(int)files[i].length()];
                    fis.read( data );
                    fis.close();
                    files[i].delete();
                    fos.write( data );
                }
                fos.write("</ul></div>".getBytes());
                fos.close();
            }
        }
    }
    /**
     * Normalise a poem's name by removing unwanted junk and equating
     * names for the same poem using the config file
     * @param raw the raw poem name from the src
     * @return a normalised poem name or the empty string if not a poem
     */
    private String normaliseName( String raw )
    {
        return raw.replaceAll(":|\\?|\"","");
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
        // the TEI element of each poem
        Element tei = null;
        // the text node of each XML poem
        Element text = null;
        String title = null;
        String hVersion = null;
        String hWork = null;
        while ( child != null )
        {
            if ( child.getNodeType()==Node.COMMENT_NODE 
                && child.getTextContent().trim().startsWith("***") )
            {
                if ( tei != null&&title!=null&&hWork !=null&&hVersion!=null )
                {
                    String srcName = src.getName();
                    if ( hWork != null && works.containsKey(hWork) )
                        title = works.get( hWork );
                    addPoem(title,srcName,hWork,hVersion,tei);  
                    hVersion = hWork = null;
                }
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
                    String type = ((Element)clone).getAttribute("type");
                    String attr = ((Element)clone).getAttribute("xml:id");
                    if ( type != null && type.toLowerCase().equals("hversion") 
                            && attr != null && attr.startsWith("H") )
                    {
                        hVersion = attr;
                        hWork = hGetWork( hVersion );
                    }
                    String desc = extractDescription( clone );
                    if ( desc != null )
                    {
                        String sName = simpleName(src.getName());
                        Anthology anth = anthologies.get(sName);
                        if ( anth != null )
                        {
                            if ( !anth.descriptionSet() )
                                anth.setDescription( desc );
                            if ( !anth.titleSet() )
                                anth.setTitle( sName );
                            if ( !versions.containsKey(sName) )
                                versions.addVersion( sName, desc );
                        }
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
        {
            String srcName = src.getName();
            if ( hWork != null && works.containsKey(hWork) )
                title = works.get( hWork );
            addPoem(title,srcName,hWork,hVersion,tei);  
        }
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
     * Append one element's children to another
     * @param elem the element to add the children to
     * @param parent the parent who will lose custody
     */
    void appendChildren( Element elem, Element parent )
    {
        Node child = parent.getFirstChild();
        while ( child != null )
        {
            elem.appendChild( child.cloneNode(true) );
            child = child.getNextSibling();
        }
    }
    /**
     * Search for MJSNotes and convert them to standard notes
     * @param elem the element to start from
     */
    void convertNotes( Document doc, Element elem )
    {
        if ( elem.getNodeName().equals("div") )
        {
            if ( elem.hasAttribute("type") )
            {
                String noteType = elem.getAttribute("type").toLowerCase();
                if ( noteType.equals("mjsnote") )
                {
                    Node child = elem.getFirstChild();
                    while ( child != null )
                    {
                        if ( child.getNodeType() == Node.ELEMENT_NODE )
                        {
                            if ( child.getNodeName().equals("p") )
                            {
                                Element note = doc.createElement("note");
                                note.setAttribute("resp", "MJS");
                                appendChildren( note, (Element)child );
                                Node parent = elem.getParentNode();
                                if ( parent != null )
                                {
                                    parent.replaceChild( note, elem );
                                }
                            }
                        }
                        child = child.getNextSibling();
                    }
                    return;
                }
            }
        }
        else if ( elem.getNodeName().equals("floatingText"))
        {
            doc.renameNode( elem, "", "div" );
        }
        // recurse through children
        Node child = elem.getFirstChild();
        while ( child != null )
        {
            if ( child.getNodeType()==Node.ELEMENT_NODE )
                convertNotes( doc, (Element)child );
            child = child.getNextSibling();
        }
    }
    private Document readXML() throws Exception
    {
        FileInputStream fis = new FileInputStream( src );
        byte[] data = new byte[(int)src.length()];
        fis.read( data );
        fis.close();
        String tei = new String( data, "UTF-8" );
        poems = new FileMap( folder );
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        StringReader sr = new StringReader( tei );
        InputSource is = new InputSource( sr );
        return db.parse( is ); 
    }
    /**
     * Parse the input file
     */
    public void parse() throws Exception
    {
        try
        {
            Anthology anth = new Anthology( miscDir, 
                simpleName(src.getName()), linkBase, MISC_SERVER );
            anthologies.put(simpleName(src.getName()),anth);
            versions = new VersionsDocument( folder );
            versions.internalise();
            Document doc = readXML();
            Element root = doc.getDocumentElement();
            convertNotes( doc, root );
            split( doc, root );
            poems.save( folder, anth, useSubFolders );
            boolean res = true;
            if ( !miscDir.exists() )
                res = miscDir.mkdir();
            if ( !res )
                throw new Exception("Failed to create anthologies dir");
            anth.externalise();
            versions.externalise();
            // write archive file
            File arc = new File( topLevelFolder, "archive.conf" );
            if ( !arc.exists() )
            {
                FileOutputStream fos = new FileOutputStream( arc );
                byte[] jsonData = archive.getBytes();
                fos.write( jsonData );
                fos.close();
            }
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
                if ( a.join )
                    a.joinAnthologies();
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
