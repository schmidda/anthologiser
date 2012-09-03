/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package anthologiser;

/**
 *
 * @author desmond
 */
public class PunctIgnoreString implements Comparable<PunctIgnoreString>
{
    String str;
    public PunctIgnoreString( String str )
    {
        this.str = str;
    }
    /**
     * Compare two string ignoring punctuation
     * @param other the other string to compare to
     * @return 
     */
    @Override
    public int compareTo( PunctIgnoreString other )
    {
        int i=0;
        int j = 0;
        while ( i<str.length()&&j<other.str.length() )
        {
            // point to next letter/digit
            while ( i<str.length()
                && !Character.isLetter(str.charAt(i)) 
                && !Character.isDigit(str.charAt(i)) )
                i++;
            while ( j<other.str.length()
                && !Character.isLetter(other.str.charAt(j)) 
                && !Character.isDigit(other.str.charAt(j)) )
                j++;
            if ( i == str.length() && j < other.str.length() )
                return -1;
            else if ( j == other.str.length() && i < str.length() )
                return 1;
            else if ( j == other.str.length() && i == str.length() )
                return 0;
            else if ( str.charAt(i) < other.str.charAt(j) )
                return -1;
            else if ( str.charAt(i) > other.str.charAt(j) )
                return 1;
            i++;
            j++;
        }
        return (i==str.length() && j==other.str.length())?1:0;
    }
}
