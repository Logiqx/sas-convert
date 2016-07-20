/**
 * Copyright (c) 2015 The Hyve
 * This file is distributed under the MIT License (see accompanying file LICENSE).
 */
package thehyve.sas;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.HelpFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVWriter;

import com.epam.parso.Column;
import com.epam.parso.SasFileProperties;
import com.epam.parso.SasFileReader;
import com.epam.parso.impl.SasFileReaderImpl;

/**
 * Command-line utility to convert files in SAS7BDAT format to 
 * comma-separated format (CSV). 
 * Based on the Parso library ({@link http://lifescience.opensource.epam.com/parso.html})
 * and opencsv ({@link http://opencsv.sourceforge.net/})
 * 
 * @author gijs@thehyve.nl
 */
public class Convert {

    public static final String USAGE = "sas-convert-0.2.jar [option] file.sas [file.csv]";
    Logger log = LoggerFactory.getLogger(getClass());

    public void convert(InputStream in, OutputStream out, int max) throws IOException {
        SasFileReader reader = new SasFileReaderImpl(in);
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(out));
        Object[] data;
        List<Column> columns = reader.getColumns();
        String[] outData = new String[columns.size()];
        // Writing column names
        for(int i=0; i < columns.size(); i++) {
            outData[i] = columns.get(i).getName();
        }
        writer.writeNext(outData);

        try {
            long rowCount = 0;
            while((data = reader.readNext()) != null) {
                if (max != -1 && rowCount >= max) {
                    break;
                }
                assert(columns.size() == data.length);
                for(int i=0; i < data.length; i++) {
                    outData[i] = data[i] == null ? "" : data[i].toString();
                }
                writer.writeNext(outData);
                rowCount++;
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    public void test(InputStream in) throws IOException {
        Date start = new Date();
        SasFileReader reader = new SasFileReaderImpl(in);
        Object[] data;
        SasFileProperties properties = reader.getSasFileProperties();
        log.info("Reading file " + properties.getName());
        log.info(properties.getRowCount() + " rows.");
        List<Column> columns = reader.getColumns();
        String[] outData = new String[columns.size()];

        try {
            log.info("Testing data...");
            long rowCount = 0;
            while((data = reader.readNext()) != null) {
                assert(columns.size() == data.length);
                rowCount++;
            }
            log.info("Done testing data.");
            log.info(rowCount + " rows tested.");
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        Date end = new Date();
        log.info("Testing took {} seconds.", (end.getTime() - start.getTime())/1000);
    }
    
    public void columns(InputStream in, OutputStream out) throws IOException {
        SasFileReader reader = new SasFileReaderImpl(in);
        CSVWriter writer = new CSVWriter(new OutputStreamWriter(out));
        Object[] data;
        List<Column> columns = reader.getColumns();
        String[] outData = new String[6];
        // Writing column names
        outData[0] = "Id";
        outData[1] = "Name";
        outData[2] = "Label";
        outData[3] = "Format";
        outData[4] = "Type";
        outData[5] = "Length";
        writer.writeNext(outData);

        try {
            for(int i=0; i < columns.size(); i++) {
                outData[0] = Integer.toString(columns.get(i).getId());
                outData[1] = columns.get(i).getName();
                outData[2] = columns.get(i).getLabel();
                outData[3] = columns.get(i).getFormat();
                outData[4] = columns.get(i).getType().getSimpleName();
                outData[5] = Integer.toString(columns.get(i).getLength());
                writer.writeNext(outData);
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
    
    public void info(InputStream in) throws IOException {
        SasFileReader reader = new SasFileReaderImpl(in);
        SasFileProperties properties = reader.getSasFileProperties();
        log.info("Reading file " + properties.getName());
        log.info(properties.getRowCount() + " rows.");

        List<Column> columns = reader.getColumns();
        log.info(columns.size() + " columns.");
    }
    
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption("c", "columns", false, "List Columns");
        options.addOption("h", "help", false, "Help");
        options.addOption("l", "limit", true, "Limit Records");
        options.addOption("i", "info", false, "Summary Information");
        options.addOption("t", "test", false, "Test Only");
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cl = parser.parse(options, args);
            if (cl.hasOption("help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(USAGE + "\n", options);
                return;
            }
            List<String> argList = cl.getArgList();
            if (argList.size() < 1) {
                System.err.printf("Too few parameters.\n");
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(USAGE + "\n", options);
                return;
            }
            try {
                FileInputStream fin = new FileInputStream(argList.get(0));
                Convert converter = new Convert();
                if (cl.hasOption("info")) {
                    converter.info(fin);
                }
                else if (cl.hasOption("test")) {
                    converter.test(fin);
                }
                else if (cl.hasOption("columns")) {
                    if (argList.size() == 1) {
                        converter.columns(fin, System.out);
                    }
                    else {
                        FileOutputStream fout = new FileOutputStream(argList.get(1));
                        converter.columns(fin, fout);
                        fout.close();
                    }
                }
                else {
                    int max = -1;                 
                    if (cl.hasOption("limit")) {
                        max = Integer.parseInt(cl.getOptionValue("limit"));                        
                    }
                    if (argList.size() == 1) {
                        converter.convert(fin, System.out, max);
                    }
                    else {
                        FileOutputStream fout = new FileOutputStream(argList.get(1));
                        converter.convert(fin, fout, max);
                        fout.close();
                    }
                }
                fin.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(USAGE + "\n", options);
            e.printStackTrace();
            return;
        }
    }

}
