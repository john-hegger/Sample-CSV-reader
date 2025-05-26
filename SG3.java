


import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;


public class SG3 {
    //GLOBAL VARIABLES
    private final static Scanner scan = new Scanner(System.in);
    private static String filename;

    private static final String SOURCE_PATH = Paths.get(".").toAbsolutePath().normalize().toString();

    public static void main(String[] args) throws Exception {
        //Calls for introduction to be printed for user
        printIntroduction();

        //Asks for filename and loops if invalid
        while(!getValidFileName())  {
            System.out.println("Please enter the name of an existing CSV File");
        }

        File file = new File(SOURCE_PATH, filename);
        DatedTable Dtable = DatedTable.from(file);

        System.out.println("The species are" + Dtable.names);
        System.out.println("Please press ENTER to continue.");
        scan.nextLine();  //awaits the user input to press ENTER

        writeToFile("Species.txt",Dtable.names);
        writeToFile("DatedData.txt",Dtable.dates);



        DatedTable Presence = computePresenceAbsence(Dtable);


		scan.close(); //save this for the very end of the main function!
    }
    //FUNCTIONS

    //Explains what the program does.
    private static void printIntroduction() {
        System.out.println("This program will read a .csv file chosen from user input, and create three files containing the data.");
        System.out.println("One file will hold the name of the species in the file, another will list the dates found in the data,");
        System.out.println(" and the last file will tell which cells have data in them and which ones have '0'.");
        System.out.println("Finally, the program will create a heat map of all the data in the original .csv file.");
    }

    private static boolean getValidFileName() {
        System.out.println("Enter the name of a CSV File:");
        filename = scan.nextLine().trim();
        File file = new File(SOURCE_PATH, filename); // this is just for the check, file will be pulled from main
        if (!file.exists()) {
            System.out.println("ERROR: File not found. Please try again.");
        }
        else if (!filename.toLowerCase().endsWith(".csv"))   {
            System.out.println("ERROR: File is not a CSV file. Please try again.");
        }
        return filename.toLowerCase().endsWith(".csv") && file.exists() && file.isFile();
    }

    /*
     * Computes the Presence-Absence equivalent of the given DatedTable.
     * The logic is as follows:
     *     For each element in Dtable,
     *         if that element is a 0, the corresponding element in the Presence-Absence is 0.
     *         If that element is 1 or more, the corresponding element in the Presence-Absence is 1.
     * Input: a DatedTable 'Dtable'
     * Output: DatedTable containing the Presence-Absence of 'Dtable'
     */
    public static DatedTable computePresenceAbsence(DatedTable Dtable) {
        return Dtable.map(num -> (num > 0) ? 1d : 0d);
    }


    
    /*    
    * this method is used to handle writing the two simple files, Species.txt and DatedData.txt. It's modular enough to handle making any file 
    * that meets the requirements for its constructor. Also includes error handling if the file is not opened successfully.*/
    private static void writeToFile(String filename, List<String> data) {
        try (PrintWriter writer = new PrintWriter(new File(filename))) {
            for (String item : data) {
                writer.println(item);
            }
            System.out.println("Data written to " + filename);
        } catch (IOException e) {
            System.out.println("Error writing to " + filename);
        }
    }

    /*
     * DatedTable is a class encapsulating a table of numerical data
     * with a text label on each column, and a date label on each row.
     *
     * It possesses 2 constructors:
     *   DatedTable(List<List<String>> table); Builds a table from a two-dimensional list of data
     *   DatedTable(List<String> name, List<String> dates, List<List<Double>> data ); builds a table
     *       from the given parts
     *
     * DatedTables can also be constructed using the DatedTable.from(File file) method, if the file
     * is a CSV file
     */

    public static class DatedTable {

        //The text labels of each column
        private List<String> names;

        //The date labels of each row (as text)
        private List<String> dates;

        //The numerical data contained in each row and column of the table
        private List<List<Double>> data;


        /*
         * Constructs a table from the given CSV file, by parsing the CSV data into a
         * List<List<String>> and passing it to the DatedTable(List<List<String>> table)
         * constructor.
         *
         * Input: A CSV file
         * Output: A DatedTable containing the data from the CSV file.
         */
        public static DatedTable from(File file) throws Exception {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            List<List<String>> table = new ArrayList<>();
            try {
                String line = reader.readLine();
                List<String> row = Arrays.asList(line.split(","));
                table.add(row);
                line = reader.readLine(); // header needs no further processing

                while(line != null){
                    row = Arrays.asList(line.split(","));
                    table.add(row);
                    line = reader.readLine();
                }
                reader.close();

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            return new DatedTable(table);
        }

        /*
         * A private method used by the DatedTable to parse a List<List<String>>
         * and extract the name labels.
         * If you need to use this method, consider calling the
         * DatedTable(List<List<String>> table) constructor instead.
         */
        private void computeNames(List<List<String>> table) throws Exception{

            if (table.isEmpty()){
                throw new Exception("Table is missing first row.");
            }
            if (!table.getFirst().getFirst().isEmpty()){
                throw new Exception("Table must have blanks on first row and column.");
            }
            this.names = table.getFirst().subList(1, table.getFirst().size());
        }

        /*
         * A private method used by the DatedTable to parse a List<List<String>>
         * and extract the date labels.
         * If you need to use this method, consider calling the
         * DatedTable(List<List<String>> table) constructor instead.
         */
        private void computeDates(List<List<String>> table) throws Exception{

            if (table.size() <= 1){
                throw new Exception("Table is missing data rows.");
            }
            this.dates = new ArrayList<>();

            for (int i = 1; i < table.size(); i++){
                this.dates.add(extractDate(table.get(i), i));
            }
        }

        /*
         * A private method used by the DatedTable to parse a List<List<String>>
         * and extract the table data.
         * If you need to use this method, consider calling the
         * DatedTable(List<List<String>> table) constructor instead.
         */
        private void computeData(List<List<String>> table) throws Exception{
            List<List<String>> rawData = table.subList(1, table.size()).stream().map(row -> row.subList(1, row.size())).toList();

            if (rawData.size() > 999){
                throw new Exception(String.format("Table has %d too many rows. (currently %s, must be less than 1000)", rawData.size() - 1000, 1000));
            }

            this.data = new ArrayList<>();

            for (int i = 0; i < rawData.size(); i++){
                List<Double> row = new ArrayList<>();
                for (int j = 0; j < rawData.get(i).size(); j++){
                    Scanner scanner = new Scanner(rawData.get(i).get(j));
                    if (!scanner.hasNextDouble()){
                        throw new Exception(String.format("Value %s at row %d, column %d is not a valid number.",rawData.get(i).get(j) ,i, j));
                    }
                    row.add(scanner.nextDouble());
                }
                this.data.add(row);
            }
        }

        /*
         * A private method used by the DatedTable to extract date labels from
         * rows
         * If you need to use this method, consider calling the
         * DatedTable(List<List<String>> table) constructor instead.
         */
        private static String extractDate(List<String> table, int row) throws Exception{
            String result = table.getFirst();
            if (!result.matches("^[0-1]?[0-9]/[0-9][0-9]/[0-9][0-9][0-9][0-9]$")){
                throw new Exception(String.format("Date %s is an invalid date in row %d.", result, row));
            }
            return result;
        }

        /*
         * Constructs a DatedTable from a List<List<String>> table.
         * The first row is assumed to contain all names, and the
         * first column is assumed to contain all dates. Element[0,0]
         * is ignored. The remainder of the data is assumed to be numerical.
         */
        public DatedTable(List<List<String>> table) throws Exception {
            computeNames(table);
            computeDates(table);
            computeData(table);
        }

        /*
         * Constructs a DatedTable from its label lists and numerical data.
         */
        public DatedTable(List<String> name, List<String> dates, List<List<Double>> data ) {
            this.names = name;
            this.dates = dates;
            this.data = data;
        }

        /*
         * Gets the list of text labels on each column
         */
        public List<String> getNames() {
            return this.names;
        }

        /*
         * Gets the list of date labels on each row
         */
        public List<String> getDates() {
            return this.dates;
        }

        /*
         * Gets the two-dimensional list of numerical data
         */
        public List<List<Double>> getData() {
            return this.data;
        }

        /*
         * Creates a new DatedTable where each data element is modified by the given function.
         * Input: A function 'f' that accepts one double and returns another
         * Output: A new DatedTable where each data element is a result of 'f' when passed this
         *     object's corresponding elements
         */
        public DatedTable map(Function<Double, Double> f) {
            List<List<Double>> mapped = this.data.stream().map(row -> row.stream().map(f).toList()).toList();
            return new DatedTable(List.copyOf(this.names), List.copyOf(this.dates), mapped);
        }

    }
}