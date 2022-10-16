/*
 * InsertRow.java
 *
 * DBMS Implementation
 */

import java.io.*;
import java.util.Arrays;

/**
 * A class that represents a row that will be inserted in a table in a
 * relational database.
 *
 * This class contains the code used to marshall the values of the
 * individual columns to a single key-value pair.
 */
public class InsertRow {
    private Table table;           // the table in which the row will be inserted
    private Object[] columnVals;   // the column values to be inserted
    private RowOutput keyBuffer;   // buffer for the marshalled row's key
    private RowOutput valueBuffer; // buffer for the marshalled row's value
    private int[] offsets;         // offsets for header of marshalled row's value
    
    /** Constants for special offsets **/
    /** The field with this offset has a null value. */
    public static final int IS_NULL = -1;
    
    /** The field with this offset is a primary key. */
    public static final int IS_PKEY = -2;
    
    /**
     * Constructs an InsertRow object for a row containing the specified
     * values that is to be inserted in the specified table.
     *
     * @param  t  the table
     * @param  values  the column values for the row to be inserted
     */
    public InsertRow(Table table, Object[] values) {
        this.table = table;
        this.columnVals = values;
        this.keyBuffer = new RowOutput();
        this.valueBuffer = new RowOutput();
        
        // Note that we need one more offset than value,
        // so that we can store the offset of the end of the record.
        this.offsets = new int[values.length + 1];
    }
    
    /**
     * Takes the collection of values for this InsertRow
     * and marshalls them into a key/value pair.
     * 
     * (Note: We include a throws clause because this method will use 
     * methods like writeInt() that the RowOutput class inherits from 
     * DataOutputStream, and those methods could in theory throw that 
     * exception. In reality, an IOException should *not* occur in the
     * context of our RowOutput class.)
     */
    public void marshall() throws IOException {
        /* 
         * PS 2: Implement this method. 
         * 
         * Feel free to also add one or more private helper methods
         * to do some of the work (e.g., to fill in the offsets array
         * with the appropriate offsets).
         */
        //write the key to the key buffer
        this.writeKey(columnVals, keyBuffer);
        //write offsets
        this.writeOffsets(columnVals, offsets);
        //write the values to the value buffer
        this.writeValues(columnVals, offsets, valueBuffer);
    }
        
    private void writeValues(Object[] vals, int[] offs, RowOutput buffer) throws IOException {
        //write offsets to buffer
        for(int i = 0; i < offs.length; i++){
            if(offs[i] == -2){
                buffer.writeByte(-1);
                buffer.writeByte(-2);
            }else if(offs[i] == -1){
                buffer.writeByte(-1);
                buffer.writeByte(-1);
            }else{
                buffer.writeShort(offs[i]);
            }
        }
        
        //write vals to buffer
        for(int i = 0; i < vals.length; i++){
            if((table.primaryKeyColumn().getIndex() != i) && (vals[i] != null)){
                processRowOutputItem(table.getColumn(i), vals[i], buffer);
            }
        }
    }

    private void writeOffsets(Object[] vals, int[] buffer) {
        int valFirstOffset = (vals.length + 1) * 2; //get num offsets
        Column primaryColumn = table.primaryKeyColumn(); //get primary key column if it exists
        Integer primaryIndex = primaryColumn.getIndex();
        //write offsets
        int currSum = 0;
        boolean firstWritten = false;
        for(int i = 0; i < vals.length; i++){
            Column col = table.getColumn(i);
            if(vals[i] == null){
                buffer[i] = -1;
            }else if(i == primaryIndex){
                buffer[i] = -2;
            }else{
                if(!firstWritten){ 
                    currSum = valFirstOffset;
                    firstWritten = true;
                    buffer[i] = currSum;
                }
                currSum = processColumnOffset(vals, i, col.getType(), currSum);
                buffer[i+1] = currSum;
            }
        }
    }

    private int processColumnOffset(Object[] vals, int i, Integer currColumnType, int currSum) {
        if(vals[i] == null){
            return currSum;
        }

        switch(currColumnType){
            case 0:
                currSum += 4;
                break;
            case 1:
                currSum += 8;
                break;
            case 2:
            case 3:
                currSum += ((String)vals[i]).length();
                break;
            case 11:
                currSum += 8;  
                break;
            default:
                System.out.println("Unknown column type...");
                break;
        }
        return currSum;
    }

    private void writeKey(Object[] vals, RowOutput buffer) throws IOException {
        Column pCol = table.primaryKeyColumn();
        int pColi = pCol.getIndex();
        Object value = vals[pColi]; //get col value
        processRowOutputItem(pCol, value, buffer);
    }

    private void processRowOutputItem(Column pCol, Object value, RowOutput buffer) throws IOException {
        switch(pCol.getType()){
            case 0:
                buffer.writeInt(((Integer)value).intValue());
                break;
            case 1:
                buffer.writeDouble(((Double)value).doubleValue());
                break;
            case 2:
            case 3:
                buffer.writeBytes(((String)value));
                break;
            case 11:
                buffer.writeLong((long)value);
                break;
            default:
                System.out.println("Unkown column type!");
        }
    }

    /**
     * Returns the RowOutput used for the key portion of the marshalled row.
     *
     * @return  the key's RowOutput
     */
    public RowOutput getKeyBuffer() {
        return this.keyBuffer;
    }
    
    /**
     * Returns the RowOutput used for the value portion of the marshalled row.
     *
     * @return  the value's RowOutput
     */
    public RowOutput getValueBuffer() {
        return this.valueBuffer;
    }
    
    /**
     * Returns a String representation of this InsertRow object. 
     *
     * @return  a String for this InsertRow
     */
    public String toString() {
        return "offsets: " + Arrays.toString(this.offsets)
             + "\nkey buffer: " + this.keyBuffer
             + "\nvalue buffer: " + this.valueBuffer;
    }
}
