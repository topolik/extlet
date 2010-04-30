<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.File" %>
<%@ page import="java.io.FileInputStream" %>
<%@ page import="java.io.InputStream" %>
<%@ page import="java.io.InputStreamReader" %>
<%@ page import="java.io.PrintWriter" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>

<h2>Version information</h2>
<pre>
<%
        File f = new File(getServletContext().getRealPath("/META-INF/maven/"));
        ArrayList dirs = new ArrayList();
        dirs.addAll(Arrays.asList(f.listFiles()));
        while (dirs.size() > 0) {
            File nestedFile = (File) dirs.remove(0);
            if (nestedFile.isDirectory()) {
                dirs.addAll(Arrays.asList(nestedFile.listFiles()));
            } else {
                if (nestedFile.getName().equals("pom.properties")) {
                    InputStream is = new FileInputStream(nestedFile);
                    if (is != null) {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            while (reader.ready()) {
                                String line = reader.readLine();
                                if(line.length() > 0){
                                    out.println(line);
                                    out.println("<br />");
                                }
                            }
                        } finally {
                            try {
                                is.close();
                            } catch (Exception e) {
                                e.printStackTrace(new PrintWriter(out));
                            }
                        }
                    } else {
                        out.println("Configuration file for " + nestedFile + " not found!");
                    }
                }
            }
        }
%>
</pre>
