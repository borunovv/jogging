package com.borunovv.core.testing.fixtures.yaml;


import com.borunovv.core.util.IOUtils;
import org.dbunit.dataset.*;
import org.dbunit.dataset.datatype.DataType;
import org.ho.yaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

// Взято тут: http://jyaml.sourceforge.net/yaml4dbunit.html
public class YamlDataSet implements IDataSet {

    private Map<String, MyTable> tables = new HashMap<String, MyTable>();
    private List<String> tableOrder = new LinkedList<String>();

    public YamlDataSet(InputStream fileStream) {
        this(convertStreamToString(fileStream));
    }

    public YamlDataSet(String content) {
        Map<String, List<Map>> data = (Map<String, List<Map>>) Yaml.load(content);
        for (Map.Entry<String, List<Map>> ent : data.entrySet()) {
            String tableName = ent.getKey();
            List<Map> rows = ent.getValue();
            createTable(tableName, rows);
        }
        determineTableOrder(content);
    }

    private void determineTableOrder(String content) {
        tableOrder.clear();
        Map<Integer, String> offsets = new TreeMap<Integer, String>();
        for (String name : tables.keySet()) {
            int index = content.indexOf("\n" + name + ":");
            if (index == -1) {
                index = content.indexOf(name + ":");
            }
            if (index == -1) {
                throw new RuntimeException("Can't determine order for table '" + name + "'.");
            }
            offsets.put(index, name);
        }
        for (Map.Entry<Integer, String> pair : offsets.entrySet()) {
            tableOrder.add(pair.getValue());
        }
    }

    private MyTable createTable(String name, List<Map> rows) {
        MyTable table;
        if (rows != null) {
            table = new MyTable(name,
                    rows.size() > 0 ?
                            new ArrayList<String>(rows.get(0).keySet()) :
                            null);
            for (Map values : rows) {
                table.addRow(values);
            }
        } else {
            table = new MyTable(name, null);
        }

        tables.put(name, table);

        return table;
    }

    public ITable getTable(String tableName) throws DataSetException {
        return tables.get(tableName);
    }

    public ITableMetaData getTableMetaData(String tableName) throws DataSetException {
        return tables.get(tableName).getTableMetaData();
    }

    public String[] getTableNames() throws DataSetException {
        return tableOrder.toArray(new String[tableOrder.size()]);
    }

    public ITable[] getTables() throws DataSetException {
        ITable[] result = new ITable[tables.size()];
        int i = 0;
        for (String name : tableOrder) {
            result[i++] = tables.get(name);
        }
        return result;
    }

    public ITableIterator iterator() throws DataSetException {
        return new DefaultTableIterator(getTables());
    }

    public ITableIterator reverseIterator() throws DataSetException {
        return new DefaultTableIterator(getTables(), true);
    }

    private static String convertStreamToString(InputStream fileStream) {
        try {
            return IOUtils.inputStreamToString(fileStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isCaseSensitiveTableNames() {
        return false;
    }


    private static class MyTable implements ITable {
        private String name;
        private List<Map> data;
        private ITableMetaData meta;

        public MyTable(String name, List<String> columnNames) {
            this.name = name;
            this.data = new ArrayList<Map>();
            meta = createMeta(name, columnNames);
        }

        private ITableMetaData createMeta(String name, List<String> columnNames) {
            Column[] columns = new Column[0];
            if (columnNames != null) {
                columns = new Column[columnNames.size()];
                for (int i = 0; i < columnNames.size(); i++)
                    columns[i] = new Column(columnNames.get(i), DataType.UNKNOWN);
            }
            return new DefaultTableMetaData(name, columns);
        }

        public int getRowCount() {
            return data.size();
        }

        public ITableMetaData getTableMetaData() {
            return meta;
        }

        public Object getValue(int row, String column) throws DataSetException {
            if (data.size() <= row)
                throw new RowOutOfBoundsException("" + row);
            return data.get(row).get(column.toUpperCase());
        }

        public void addRow(Map values) {
            data.add(convertMap(values));
        }

        private Map convertMap(Map<String, Object> values) {
            Map ret = new HashMap();
            for (Map.Entry<String, Object> ent : values.entrySet()) {
                ret.put(ent.getKey().toUpperCase(), ent.getValue());
            }
            return ret;
        }
    }
}
