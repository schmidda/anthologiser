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
import calliope.json.JSONDocument;
import org.htmlparser.Parser;
import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.ArrayList;
import org.htmlparser.util.NodeList;
/**
 * Represent an anthology file in HTML format. Preserve the order of poems 
 * added to the anthology. Structure is:
 * anthologies
 *     |
 *     parent ----- [HTML]
 *                    |
 *                    -------name.json
 * @author desmond 15/5/2012
 */
public class Anthology 
{
    String linkBase;
    String server;
    /** anthology title */
    String title;
    /** anthology description - usually MS library entry */
    String description;
    File src;
    /** anthology file */
    File htmlFile;
    /** order-preserving array */
    ArrayList<String> order;
    /** the name of this anthology */
    String name;
    /** relative links to resources in database, ensures uniqueness */
    HashMap<String,String> links;
    /**
     * Read in an existing anthology or create an empty one
     * @param dir the misc directory where the anthology files reside
     * @param simpleName short name of the anthology
     * @param linkBase the prefix for all links
     * @param server url minus the linkBase
     */
    public Anthology( File dir, String simpleName, String linkBase, 
        String server ) throws Exception
    {
        links = new HashMap<String,String>();
        order = new ArrayList<String>();
        this.linkBase = linkBase;
        this.name = simpleName;
        this.server = server;
        title = simpleName;
        File temp = new File( dir, linkBase+"/"+"anthologies" );
        boolean res = true;
        if ( !temp.exists() )
            res = temp.mkdirs();
        if ( !res )
            throw new Exception("Couldn't create "+temp.getPath());
        htmlFile = new File( temp, simpleName );
        src = htmlFile;
        internalise();
    }
    /**
     * Set this anthology's title
     * @param title the name for the collection as stated in the source
     */
    public void setTitle( String title )
    {
        this.title = title;
    }
    /**
     * Has the description already been set?
     * @return true if it has
     */
    public boolean descriptionSet()
    {
        return description != null && description.length()>0;
    }
    /**
     * Has the title already been set?
     * @return true if it has
     */
    public boolean titleSet()
    {
        return title != null && title.length()>0;
    }
    /**
     * Get the description string
     * @param content description of the anthology as stated in the source
     */
    public void setDescription( String content )
    {
        this.description = content;
    }
    /**
     * Get the prefix for all links
     * @return a String
     */
    String getLinkBase()
    {
        return linkBase;
    }
    /**
     * Add a work name to the anthology
     * @param name name of a work
     * @param link link to the resource
     */
    public void addItem( String name, String link )
    {
        // avoid duplicates, like hashmap
        if ( !links.containsKey(name) )
            order.add( name );
        links.put( name, link );
    }
    /**
     * Write out this anthology
     */
    void externalise() throws Exception
    {
        boolean res = true;
        // construct the html for one list
        StringBuilder sb = new StringBuilder();
        sb.append("<li>");
        sb.append( description );
        sb.append( "<ul>\n" );
        for ( int i=0;i<order.size();i++ )
        {
            String key = order.get( i );
            sb.append("<li><a href=\"");
            sb.append(server);
            sb.append("mvdsingle?DOCID=");
            String value = links.get(key);
            sb.append( value );
            sb.append("\">");
            sb.append( key );
            sb.append( "</a></li>\n" );
        }
        sb.append("</ul></li>");
        // write out the text
        if ( !src.exists() )
        {
            res = true;
            if ( !src.getParentFile().exists() )
            {
                res = src.getParentFile().mkdirs();
                if ( !res )
                    throw new Exception("failed to create "+src.getParent());
            }
            res = src.createNewFile();
            if ( !res )
                throw new Exception("failed to create "+src.getName());
        }
        FileOutputStream fos = new FileOutputStream( src );
        fos.write( sb.toString().getBytes("UTF-8") );
        fos.close();
    }
    File getAnthologiesDir()
    {
        return htmlFile.getParentFile();
    }
    /**
     * Read in an anthology OR just specify where you want it to be
     * @param src the source anthology file
     * @param msrc the markup source, in same folder ideally
     * @throws Exception 
     */
    private void internalise() throws Exception
    {
        if ( src.exists() )
        {
            File confFile = new File( src.getParentFile(),"config.conf");
            if ( confFile.exists() )
            {
                JSONDocument conf = JSONDocument.internalise( confFile, "UTF-8" );
                this.title = (String)conf.get( JSONKeys.TITLE );
            }
            FileInputStream fis = new FileInputStream( src );
            int len = (int)src.length();
            byte[] data = new byte[len];
            fis.read( data );
            fis.close();
            String html = new String(data,"UTF-8");
            Parser parser = Parser.createParser(html, "UTF-8");
            NodeList list = parser.parse (null);
            int state = 0;
            String link = "";
            for ( int i=0;i<list.size();i++ )
            {
                Node n = list.elementAt(i);
                switch ( state )
                {
                    case 0:
                        if ( n instanceof Tag )
                        {
                           Tag t = (Tag)n;
                            if ( !t.isEndTag() )
                            {
                                if ( t.getTagName().equals("h1") )
                                state = 1;
                                else if ( t.getTagName().equals("h2") )
                                    state = 2;
                                else if ( t.getTagName().equals("p") )
                                    state = 3;
                            }
                        }
                        break;
                    case 1:
                        if ( n instanceof Tag && ((Tag)n).isEndTag() )
                            state = 0;
                        else if ( n instanceof Text )
                        {
                            title = ((Text)n).getText();
                            state = 0;
                        }
                        break;
                    case 2:
                        if ( n instanceof Tag && ((Tag)n).isEndTag() )
                            state = 0;
                        else if ( n instanceof Text )
                        {
                            description = ((Text)n).getText();
                            state = 0;
                        }
                        break;
                    case 3:
                        if ( n instanceof Tag )
                        {
                            if ( ((Tag)n).isEndTag() )
                                state = 0;
                            else if ( ((Tag)n).getTagName().equals("a") )
                            {
                                link = ((Tag)n).getAttribute("href");
                                state = 4;
                            }
                        }
                        break;
                    case 4:
                        if ( n instanceof Text )
                        {
                            String name = ((Text)n).getText();
                            addItem( name, link );
                            state = 0;
                        }
                        else if ( n instanceof Tag && ((Tag)n).isEndTag() )
                        {
                            state = 0;
                        }
                        break;
                }
            }
            src.delete();
            // html file will be written out afresh by externalise
        }
        // not an error, just not already existing
    }
}
