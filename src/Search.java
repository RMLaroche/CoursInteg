import java.io.*;
import java.sql.*;
import java.util.*;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servlet implementation class Search
 */
@WebServlet(description = "Search servlet", urlPatterns =
{ "/web/search" })
public class Search extends HttpServlet
{
   private static final long serialVersionUID = 1L;
   public Connection connection;
   public Properties props;

   /**
    * ResultURL Class which contains necessary information for each url element
    * to be displayed
    * 
    * @author offirgolan
    * 
    */
   class ResultURL implements Comparable<ResultURL>
   {
      String url, title, description;
      int urlid, rank;

      public ResultURL(int urlid, String url, int rank, String title,
            String desc)
      {
         this.urlid = urlid;
         this.url = url;
         this.rank = rank;
         this.title = title;
         this.description = desc;
      }

      public void setRank(int rank)
      {
         this.rank = rank;
      }

      public void setTitle(String title)
      {
         this.title = title;
      }

      @Override
      public int compareTo(ResultURL arg0)
      {
         return arg0.rank - this.rank;
      }

      @Override
      public String toString()
      {
         return this.rank + "";
      }

   }

   protected void doPost(HttpServletRequest request,
         HttpServletResponse response) throws ServletException, IOException
   {
      HttpSession session = request.getSession(true);
      response.setContentType("text/html");
      PrintWriter out = response.getWriter();
      
      
      int maxRank = 0;
      int pageNum = 0;
      List<ArrayList<String>> resultList = new ArrayList<ArrayList<String>>();
      String search = new String();

      readProperties();
      try
      {
         search = request.getParameter("search_field");
         pageNum = Integer.parseInt(request.getParameter("page"));

         // Open database connection
         openConnection();

         String[] words = search.toLowerCase().split("\\s+");
         List<List<Integer>> urlIDs = new ArrayList<List<Integer>>();
         List<ResultURL> result = new ArrayList<ResultURL>();
         Crawler crawler = new Crawler(connection);

         // Get the urlList of each word
         for (String word : words)
         {
            if (crawler.wordInDB(word, Crawler.TABLE_WORD))
            {
               String list = crawler.getURLListFromDB(word, Crawler.TABLE_WORD);
               List<Integer> temp = new ArrayList<Integer>();

               // Add each individual urlID to the list
               for (String idStr : list.split(","))
               {
                  temp.add(Integer.parseInt(idStr));
               }

               urlIDs.add(temp);
            }
         }

         if (!urlIDs.isEmpty())
         {
            // Get the intersection of the urlIDs
            List<Integer> intersection = urlIDs.get(0);
            for (List<Integer> list : urlIDs)
               intersection.retainAll(list);

            // Retrieve information from DB for each urlID
            for (int urlid : intersection)
            {
               PreparedStatement pstmt = connection
                     .prepareStatement("SELECT * FROM urls WHERE urlid = ?");
               pstmt.setInt(1, urlid);
               ResultSet r = pstmt.executeQuery();
               r.next();
               int rank = r.getInt(3);
               if (rank > maxRank)
                  maxRank = rank;
               
               String url = r.getString(2);
               if(url.contains("#")) // skip url containing fragment identifiers
                  continue;
               
               // Add data to the result list
               result.add(new ResultURL(urlid, url, rank, r
                     .getString(4), r.getString(5)));
               pstmt.close();
            }

            // Update rank if the word is found in the URL's title
            for (ResultURL obj : result)
            {
               int count = 1;
               for (String word : words)
               {
                  String title = obj.title.toLowerCase();

                  if (title.contains(word.toLowerCase()))
                  {
                     // Bold the word in the title
                     int start = title.indexOf(word);
                     int end = start + word.length();
                     String titleBold = obj.title.substring(0, start) + "<b>"
                           + obj.title.substring(start, end) + "</b>"
                           + obj.title.substring(end, title.length());

                     // Update page rank
                     obj.setRank(obj.rank + maxRank + count);

                     // Update to new title
                     obj.setTitle(titleBold);
                     count++;
                  }
               }
            }

            // Sort the result collection by rank from high to low
            Collections.sort(result);

            // Convert object to String representations
            for (ResultURL obj : result)
            {
               ArrayList<String> temp = new ArrayList<String>();
               temp.add(obj.url);
               temp.add(obj.title);
               temp.add(obj.description);
               temp.add(obj.rank + "");
               temp.add(obj.urlid + "");

               resultList.add(temp);
            }
         }

         // Add resultList to request
         request.setAttribute("query", search);
         request.setAttribute("resultList", resultList);
         request.setAttribute("pageNum", pageNum);

         // Forward to viewSearch
         String nextJSP = "/results.jsp";
         RequestDispatcher dispatcher = getServletContext()
               .getRequestDispatcher(nextJSP);
         dispatcher.forward(request, response);
         connection.close();
      }

      catch (Exception e)
      {
         // TODO Auto-generated catch block
         e.printStackTrace();
      }

   }

   public void readProperties() throws IOException
   {
      props = new Properties();
      props.load(getServletContext().getResourceAsStream(
            "/WEB-INF/database.properties"));

   }

   public void openConnection() throws SQLException, IOException,
         ClassNotFoundException
   {

      String drivers = props.getProperty("jdbc.drivers");
      if (drivers != null)
         System.setProperty("jdbc.drivers", drivers);

      String url = props.getProperty("jdbc.url");
      String username = props.getProperty("jdbc.username");
      String password = props.getProperty("jdbc.password");
      String driver = "com.mysql.jdbc.Driver";

      Class.forName(driver);
      connection = DriverManager.getConnection(url, username, password);
   }

   public Set<Integer> findIntersection(List<Integer> list)
   {
      final Set<Integer> setToReturn = new HashSet<Integer>();
      final Set<Integer> set1 = new HashSet<Integer>();

      for (Integer yourInt : list)
      {
         if (!set1.add(yourInt))
         {
            setToReturn.add(yourInt);
         }
      }
      return setToReturn;
   }

}