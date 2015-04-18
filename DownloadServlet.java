package santos;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Some improvement on file download compare to methods on websites. Firstly, ranges download deal with ranges request, download continue from the point of interruption. Secondly,
 * stream should close respectively, or one close fail may affect others.
 * 
 * @author Santos Chang, with Chinese Yongqiang Zhang.
 */
public class DownloadServlet extends HttpServlet {

    private static final long serialVersionUID = 4355650437775076117L;

    /**
     * User file directory
     */
    private final String userFileDir = "F:\\userFileDir\\";

    /**
     * Default buffer size
     */
    private final int bufferSize = 1024;

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) {

        String fileName = req.getParameter("file");
        String download = req.getParameter("download");

        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            File file = new File(this.userFileDir + fileName);
            if (!file.getCanonicalPath().startsWith(userFileDir)) {
                // prevent path attack
                return;
            }
            inputStream = new FileInputStream(file);
            outputStream = resp.getOutputStream();

            // ranges download deal with ranges request.
            int available = inputStream.available();
            int begin = 0;
            int end = available - 1;
            String rangeUnit = "bytes";
            String reqRange = req.getHeader("Range");
            if (null == reqRange) {
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                String[] rangeArr = reqRange.split("=");
                rangeUnit = rangeArr[0];
                String[] region = rangeArr[1].split("-");
                begin = Integer.valueOf(region[0]);
                if (region.length > 1) {
                    end = Integer.valueOf(region[1]);
                }
            }
            int contentLength = end - begin + 1;

            // Set response header.
            if (null == download) {
                // file will open in browser
                resp.setHeader("Content-Disposition", "inline; filename=" + fileName);
            } else {
                // file will download as attachment
                resp.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            }
            resp.setContentType("image/jpeg");
            resp.setContentLength(contentLength);
            resp.setHeader("Accept-Ranges", rangeUnit);
            resp.setHeader("Content-Range", new StringBuilder(rangeUnit).append(" ").append(begin).append("-").append(end).append("/").append(available).toString());

            // Read streanm according to ranges.
            byte[] buffer = new byte[bufferSize];
            int len = 0;
            int remainLength = contentLength;
            inputStream.skip(begin);
            while (remainLength > 0) {
                if (remainLength >= bufferSize) {
                    len = bufferSize;
                } else {
                    len = remainLength;
                }

                inputStream.read(buffer);
                outputStream.write(buffer, 0, len);
                remainLength = remainLength - len;
                buffer = new byte[bufferSize];
            }

            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // stream should close respectively, or one close fail may affect others.
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (null != outputStream) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }
}
