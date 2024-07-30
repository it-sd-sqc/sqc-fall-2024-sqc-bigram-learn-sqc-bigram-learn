package edu.cvtc.bigram;

// Import statements //////////////////////////////////////////////////////////
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
  // Constants ////////////////////////////////////////////////////////////////
  public static final String DATABASE_NAME = "bigrams";
  public static final String DATABASE_PATH = DATABASE_NAME + ".db";

  private static final String DESCRIPTION = "Add bigrams in provided text files to a database.";

  private static final int TIMEOUT_STATEMENT_S = 5;
  private static final int VERSION = 1;

  // Entry point //////////////////////////////////////////////////////////////
  public static void main(String[] args) {
    ArrayList<Path> toProcess = new ArrayList<>();
    // Process arguments //////////////////////////////////////////////////////
    for (String arg : args) {
      switch (arg) {
        case "--version", "-v" -> {
          System.out.println("Version " + VERSION);
          return;
        }
        case "--reset", "-r" -> reset();
        case "--help", "-h" -> {
          System.out.println(DESCRIPTION);
          System.out.println("""
              Usage:
                java edu.cvtc.bigram.Main file […file]
              Arguments:
                --version, -v
                  Displays the version number and exits.
                --reset, -r
                  Deletes the database if present to start with an empty database.
                --help, -h
                  Displays argument help and exits.
                file
                  Specifies a path from which to extract bigrams.
              """);
          return;
        }
        default -> {
          Path p = Path.of(arg);
          try {
            if (Files.isRegularFile(p) && Files.isReadable(p)) {
              toProcess.add(p);
            }
          } catch (Exception e) {
            System.err.println("Ignoring " + p + ": " + e.getMessage());
            e.printStackTrace();
          }
        }
      } // end switch
    } // end for

    if (toProcess.isEmpty()) {
      System.err.println("Expected at least one path to a text file.");
      return;
    }

    // Database and SQL variables ///////////////////////////////////////////////
    try (Connection db = createConnection()) {
      for (Path p : toProcess) {
        String src = Files.readString(p);
        createBigrams(db, src);
      }
    }
    catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
  } // end of main

  // Database methods /////////////////////////////////////////////////////////
  public static Connection createConnection() {
    Connection result = null;
    try {
      result = DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
      System.out.println("Created connection: " + result);
      Statement command = result.createStatement();
      command.setQueryTimeout(TIMEOUT_STATEMENT_S);

      // Create the tables if needed.
      command.executeUpdate("CREATE TABLE IF NOT EXISTS words (id INTEGER PRIMARY KEY, string TEXT NOT NULL)");
      command.executeUpdate("CREATE TABLE IF NOT EXISTS bigrams (id INTEGER PRIMARY KEY, words_id INTEGER, next_words_id INTEGER)");
    }
    catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
    }
    return result;
  }

  // Deletes the database. Note that any connections created prior to resetting
  // will become invalid should they be used for calling other database-related
  // methods, leading to a DATABASE_DELETED exception.
  public static void reset() {
    try {
      Files.deleteIfExists(Path.of(DATABASE_PATH));
    } catch (Exception e) {
      System.err.println("Failed to delete database: " + e.getMessage());
      e.printStackTrace();
    }
  }

  // Returns the number of words in the database or -1 if an error
  // occurs (such as an invalid database connection).
  public static int getWordCount(Connection db) throws SQLException {
    if (db == null || db.isClosed()) {
      return -1;
    }

    Statement command = db.createStatement();
    ResultSet rows = command.executeQuery("SELECT COUNT(*) AS count FROM words");
    if (rows.next()) {
      return rows.getInt("count");
    }
    return -1;
  }

  // TODO: Identify a bug in at least one of the following methods,
  // and then create a unit test for the bug that fails.

  // Convenience function that takes an open non-null database connection
  // and a source string from which to extract bigrams. Words are defined
  // as non-whitespace characters separated by whitespace. As such,
  // "    bz'vm.,bualkc snatoheu   s" consists of three words.
  // This function updates the provided database, reusing words if possible.
  public static void createBigrams(Connection db, String src) throws SQLException {
    if (db == null || db.isClosed() || src == null) {
      return;
    }

    Scanner scanner = new Scanner(src);
    if (!scanner.hasNext()) {
      return;
    }

    int w0 = getId(db, scanner.next());
    while (scanner.hasNext()) {
      int w1 = getId(db, scanner.next());
      addBigram(db, w0, w1);
      w0 = w1;
    }
  } // end of createBigrams

  // Creates a bigram in the provided database for the provided word ids.
  // The provided word ids are assumed to indicate valid words to improve
  // efficiency; as such, they are only required to be greater than zero.
  // Use getId() to identify a word's ID in the provided database.
  public static void addBigram(Connection db, int w0, int w1) throws SQLException {
    if (db == null || db.isClosed() || w0 < 1 || w1 < 1) {
      return;
    }

    Statement command = db.createStatement();
    String query = MessageFormat.format(
        "INSERT INTO bigrams (words_id, next_words_id) VALUES ({0}, {1})",
        w0, w1);
    command.execute(query);
  } // end of addBigram

  // Returns the ID of the provided word. Words must consist of non-whitespace
  // characters, and any leading or trailing whitespace will be removed. The
  // id returned will be (1) the id of the word if it already exists in the
  // database; (2) the newly-created id if the word is added to the database; or
  // (3) -1 should an error occur (such as providing a string consisting solely
  // of whitespace).
  public static int getId(Connection db, String aWord) throws SQLException {
    int result = -1;
    if (db == null || db.isClosed() || aWord == null) {
      return result;
    }
    aWord = aWord.trim();
    if (aWord.isEmpty()) return result;

    Statement command = db.createStatement();
    String query = MessageFormat.format("""
      INSERT INTO words (string)
        SELECT ''{0}'' WHERE NOT EXISTS (
          SELECT string FROM words WHERE string = ''{0}''
        )
      """, aWord);
    command.execute(query);
    ResultSet rows = command.executeQuery(MessageFormat.format(
      "SELECT id FROM words WHERE string = ''{0}''",
      aWord));
    if (rows.next()) {
      result = rows.getInt("id");
    }

    return result;
  } // end of getId
}
