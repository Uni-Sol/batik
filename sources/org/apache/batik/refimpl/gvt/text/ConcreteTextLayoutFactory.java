/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.batik.refimpl.gvt.text;

import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.io.StreamTokenizer;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.awt.geom.Point2D;
import java.awt.font.TextLayout;
import java.awt.font.FontRenderContext;
import java.text.CharacterIterator;
import java.text.AttributedCharacterIterator;
import java.lang.reflect.Array;

import org.apache.batik.gvt.text.GVTAttributedCharacterIterator;
import org.apache.batik.gvt.text.TextSpanLayout;
import org.apache.batik.gvt.text.TextLayoutFactory;

/**
 * Factory instance that returns
 * TextSpanLayouts appropriate to AttributedCharacterIterator
 * instances.
 *
 * @see org.apache.batik.gvt.text.TextSpanLayout
 * @author <a href="bill.haneman@ireland.sun.com>Bill Haneman</a>
 * @version $Id$
 */
public class ConcreteTextLayoutFactory implements TextLayoutFactory {

   public static final int L = 0x0002;
   public static final int R = 0x0001;
   public static final int AL = 0x0005;
   public static final int RLE = 0x0011;
   public static final int RLO = 0x0031;
   public static final int LRE = 0x0010;
   public static final int LRO = 0x0030;
   public static final int B =  0x0080;
   public static final int BN = 0x0180;
   public static final int S =  0x000c;
   public static final int WS = 0x0008;
   public static final int AN = 0x0140;
   public static final int ON = 0x0102;
   public static final int EN = 0x0100;
   public static final int ET = 0x0200;

   public static String unicodeFileName = "UnicodeData.txt";

    /**
     * Returns an instance of TextSpanLayout suitable for rendering the
     * AttributedCharacterIterator.
     * @param aci the character iterator to be laid out
     */
    public TextSpanLayout createTextLayout(AttributedCharacterIterator aci,
                                                Point2D offset, 
                                                FontRenderContext frc) {
        Set keys = aci.getAllAttributeKeys();
        Set glyphPositionKeys = new HashSet();
        glyphPositionKeys.add(GVTAttributedCharacterIterator.TextAttribute.X);
        glyphPositionKeys.add(GVTAttributedCharacterIterator.TextAttribute.Y);
        glyphPositionKeys.add(GVTAttributedCharacterIterator.TextAttribute.DX);
        glyphPositionKeys.add(GVTAttributedCharacterIterator.TextAttribute.DY);
        glyphPositionKeys.add(
                       GVTAttributedCharacterIterator.TextAttribute.ROTATION);
        glyphPositionKeys.retainAll(keys);
        if (glyphPositionKeys.isEmpty()) {
            return new TextLayoutAdapter(new TextLayout(aci, frc), offset, aci);
        } else {
            char ch = aci.first();
            do {
                if (isRTL(ch)) {
                  return new TextLayoutAdapter(new TextLayout(aci, frc), offset, aci);
                }
                ch = aci.next();
            } while (ch != CharacterIterator.DONE);
            //System.out.println("Using GlyphLayout");
            return new GlyphLayout(aci, offset, frc);
        }
    }

    private boolean isRTL(char ch) {
        int bidiCode = UnicodeData.getBiDiCode(Character.getNumericValue(ch));
        switch (bidiCode) {
            case R:
            case AL:
            case RLE:
            case RLO:
                return true;
            default:
                return false;
        }
    }


    public static class UnicodeData {

        protected static int BIDI_CODE_NDX = 4;

        protected static Object[] unicodeValues = new Object[0xFF00];

        public static int getBiDiCode(int ch) {
            int i;
            try {
                 i = ((Integer) Array.get(getUnicodeData(ch), BIDI_CODE_NDX)).intValue();
            } catch (Exception e) {
                 i = 0;
            }
            return i;
        }

        public static Array getUnicodeData(int ch) {
             Array data = (Array) unicodeValues[ch];
             if (data == null) {
                 data = parseUnicodeDataEntry(ch);
                 unicodeValues[ch] = data;
             }
             return data;
        }

        public static Array parseUnicodeDataEntry(int ch) {
            Array values = (Array) Array.newInstance(Object.class, 14);
            String s = readUnicodeData(ch, unicodeFileName);
            StringTokenizer st = new StringTokenizer(s, ";");
            Array.set(values, 0, new Integer(st.nextToken())); // unicode value
            Array.set(values, 1, st.nextToken()); // character name
            Array.set(values, 2, st.nextToken()); // case
            Array.set(values, 3, st.nextToken());  //
            Array.set(values, 4, new Integer(getBiDiValue(st.nextToken()))); // BiDi value
            Array.set(values, 5, new Integer(st.nextToken())); // unicode value
            Array.set(values, 6, st.nextToken());
            Array.set(values, 7, st.nextToken());
            Array.set(values, 8, st.nextToken());
            Array.set(values, 9, st.nextToken());
            Array.set(values, 10, st.nextToken());
            Array.set(values, 11, st.nextToken());
            Array.set(values, 12, st.nextToken());
            Array.set(values, 13, st.nextToken());
            return values;
        }

        private static int getBiDiValue(String string) {
            int val = 0;
            char s[] = string.toCharArray();

            switch (s[0]) {
            case 'L':
                if (s.length > 2) {
                    switch (s[2]) {
                    case 'E':
                        val = LRE;
                        break;
                    default:
                        val = LRO;
                    }
                } else {
                   val = L;
                }
                break;
            case 'R':
                if (s.length > 2) {
                    switch (s[2]) {
                    case 'E':
                        val = RLE;
                        break;
                    default:
                        val = RLO;
                    }
                } else {
                   val = R;
                }
                break;
            case 'A':
                switch (s[2]) {
                case 'L':
                    val = AL;
                    break;
                default:
                    val = AN;
                }
                break;
            case 'B':
                if (s.length > 1) {
                    val = BN;
                } else {
                    val = B;
                }
                break;
            case 'E':
                switch (s[1]) {
                case 'N':
                    val = EN;
                    break;
                default:
                    val = ET;
                }
                break;
            case 'O':
                val = ON;
                break;
            case 'S':
                val = S;
            }
            return val;
        }

        private static String readUnicodeData(int ch, String filename) {
            String s = null;
            try {
                StreamTokenizer st = new StreamTokenizer(new FileReader(filename));
                st.resetSyntax();
                st.eolIsSignificant(true);
                for (int i=0; i<ch; ++i) {
                    s = st.toString();
                    st.nextToken(); // eol token
                    st.nextToken(); // next line
                }
                System.out.println("UnicodeData for "+ch+": "+s);
            } catch (FileNotFoundException fnfe) {
                System.out.println(fnfe);
            } catch (IOException ioe) {
                System.out.println("Error reading Unicode Database: "+ioe);
            }
            return s;
        }

    }
}