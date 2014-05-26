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

/**
 * A string that compares by ignoring punctuation
 * @author desmond
 */
public class PunctIgnoreString implements Comparable<PunctIgnoreString>
{
    String str;
    public PunctIgnoreString( String str )
    {
        this.str = str;
    }
    public int hashCode()
    {
        return str.hashCode();
    }
    /**
     * Used by HashMap to equate keys
     * @param other the other punct-ignore
     * @return 
     */
    public boolean equals( Object other )
    {
        if ( other instanceof PunctIgnoreString )
        {
            PunctIgnoreString pis = (PunctIgnoreString)other;
            return pis.str.equals(this.str);
        }
        else
            return false;
    }
    /**
     * Make it easy for debugger
     * @return a String
     */
    public String toString()
    {
        return this.str;
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
