/*
 * InsertRow.java
 *
 * DBMS Implementation
 */

import java.io.*;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collector;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
    
    // map of functions to write to buffer 
    Map<Integer, TBiConsumer<RowOutput, Object>> bufferWriteMap = new HashMap<>(); 

    
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

        //setup function map
        bufferWriteMap.put(0, (buff, val) -> buff.writeInt(((Integer)val).intValue()));
        bufferWriteMap.put(1, (buff, val) -> buff.writeDouble(((Double)val).doubleValue()));
        bufferWriteMap.put(2, (buff, val) -> buff.writeBytes((String)val));
        bufferWriteMap.put(3, (buff, val) -> buff.writeBytes((String)val));
        bufferWriteMap.put(11, (buff, val) -> buff.writeLong((long)val));
        bufferWriteMap.put(99, (buff, val) -> buff.writeShort(((Integer)val).shortValue()));
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
    public void marshall() {
        /* 
         * PS 2: Implement this method. 
         * 
         * Feel free to also add one or more private helper methods
         * to do some of the work (e.g., to fill in the offsets array
         * with the appropriate offsets).
         */
        //write the key to the key buffer
        bufferWriteMap.get(table.primaryKeyColumn().getType())
                      .accept(keyBuffer, columnVals[table.primaryKeyColumn().getIndex()]);

        //write offsets
        this.writeOffsets(columnVals, offsets);
        //write the values to the value buffer
        this.writeValues(columnVals, offsets, valueBuffer);
    }
        
    private void writeValues(Object[] vals, int[] offs, RowOutput buffer) {
        Arrays.stream(offs)
        .forEach(off -> {
            if(off == -2){
                bufferWriteMap.get(99).accept(buffer, -2); //99 maps to write short
                }else if(off == -1){
                    bufferWriteMap.get(99).accept(buffer, -1); //99 maps to write short
                }else{
                    bufferWriteMap.get(99).accept(buffer, off); //99 maps to write short
                }
            });
            
        AtomicInteger i = new AtomicInteger(0);
        Arrays.stream(vals)
              .filter(Objects::nonNull)
              .forEach(val -> {
                if(table.primaryKeyColumn().getIndex() != i.get()){
                    bufferWriteMap.get(table.getColumn(i.get()).getType()).accept(buffer, vals[i.get()]);
                }
                i.getAndIncrement();
              });
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
        HashMap<Integer, Integer> offsetMap = new HashMap<>();
        offsetMap.put(0, 4);
        offsetMap.put(1, 8);
        offsetMap.put(2, ((String)vals[i]).length());
        offsetMap.put(3, ((String)vals[i]).length());
        offsetMap.put(11, 8);

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
        return offsetMap.get(currColumnType);
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