/*
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 *
 *   Dmytro Kalpakchi, 2018
 */

package ir;

import javax.swing.JFrame;
import javax.swing.JOptionPane;


public class SpellingOptionsDialog {
    int optionsMaxLength;

    public SpellingOptionsDialog(int optionsMaxLength) {
        this.optionsMaxLength = optionsMaxLength;
    }

    /**
     *  Show the dialog window with a dropdown menu with a <code>possibilities</code>
     *  presented in the exact order as they are provided with <code>defaultOption</code>
     *  being chosen by default.
     */
    public String show(String[] possibilities, String defaultOption) {
        int numOptions = possibilities.length;

        if (optionsMaxLength > 0) {
            for (int i = 0; i < numOptions; i++) {
                int optionLength = possibilities[i].length();

                if (optionLength > optionsMaxLength) {
                    possibilities[i] = possibilities[i].substring(0, optionsMaxLength) + "...";
                }
            }
        }


        String s = (String)JOptionPane.showInputDialog(
                            null,
                            "Did you mean one of those?",
                            "Automatic Spelling Correction",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            possibilities,
                            defaultOption);
        return s;
    }
}
