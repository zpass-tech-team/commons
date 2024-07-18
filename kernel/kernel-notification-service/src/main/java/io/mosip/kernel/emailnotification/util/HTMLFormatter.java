package io.mosip.kernel.emailnotification.util;

import org.springframework.stereotype.Component;

@Component
public class HTMLFormatter {

    public String formatText(String content) {
        String formattedMessage = "<p>";

        String var1 = content.trim();
        String[] var2 = var1.replace("\\n", "#NEXT#").split("#NEXT#");
        int noofEmptyLines = 0;

        for(String v : var2) {
            if (!v.isEmpty()) {
                String var3 = v.replace("\\t", "#[TAB]#").trim().replace("#[TAB]#", "&emsp;");

                if (noofEmptyLines > 0) {
                    noofEmptyLines = 0;
                    var3 = "<br>" + var3;
                }
                formattedMessage += "<br>"+ var3;
            }

            if(v.isEmpty())
                noofEmptyLines++;
        }
        formattedMessage+="</p>";

        return  formattedMessage;
    }
}
