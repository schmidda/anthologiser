/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package anthologiser;
import java.util.StringTokenizer;
import java.util.HashSet;
/**
 * Encapsulate a harpur table entry
 * @author desmond
 */
public class Titeliser 
{
    static HashSet smallWords;
    static String[] stops = {"a","an","or","are","is","am","and",
        "the","of","at","to","in","on","with","from","that","his"};
    static
    {
        smallWords = new HashSet<String>();
        for ( int i=0;i<stops.length;i++ )
            smallWords.add( stops[i] );
        
    }
    static boolean isRomanNumeral( String word )
    {
        for ( int i=0;i<word.length();i++ )
        {
            char token = word.charAt(i);
            switch ( token )
            {
                case 'x': case 'i': case 'v': case 'l': case'c': case '.':
                    break;
                default:
                    if ( Character.isAlphabetic(token)
                        ||Character.isDigit(token) )
                       return false;
                    else
                        break;
            }
        }
        return true;
    }
    static String uppercase( String word )
    {
        StringBuilder sb = new StringBuilder();
        if ( isRomanNumeral(word) )
            sb.append(word.toUpperCase());
        else if ( word.startsWith("\"") )
        {
            if ( word.length()>2 )
            sb.append("\"");
            sb.append(Character.toUpperCase(word.charAt(1)));
            sb.append(word.substring(2));
        }
        else
        {
            if ( word.length()>0 )
                sb.append( Character.toUpperCase( word.charAt(0)) );
            if ( word.length() > 1 )
                sb.append( word.substring(1) );
        }
        return sb.toString();
    }
    /**
     * Convert to English title case
     * @param title a title in uppercase
     * @return a title with leaned upper and lowercase
     */
    static String getTitle( String title )
    {
        String lc = title.toLowerCase();
        StringTokenizer st = new StringTokenizer(lc," ");
        StringBuilder sb  = new StringBuilder();
        while ( st.hasMoreTokens() )
        {
            String word = st.nextToken();
            if ( word.contains("-") )
            {
                String[] parts = word.split("-");
                for ( int i=0;i<parts.length;i++ )
                {
                    String uc = uppercase(parts[0] );
                    sb.append(uc);
                    if ( i < parts.length-1 )
                        sb.append("-");
                }
            }
            else if ( !smallWords.contains(word)||sb.length()==0 )
            {
                sb.append( uppercase(word) );
            }
            else
                sb.append( word );
            if ( st.hasMoreTokens() )
                sb.append(" ");
        }
        return sb.toString();
    }
}
