package com.borunovv.jogging.timings.filter;

import com.borunovv.core.service.AbstractService;
import com.borunovv.core.util.Assert;
import com.borunovv.core.util.CollectionUtils;
import com.borunovv.core.util.StringUtils;
import com.borunovv.core.util.TimeUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FilterCompiler extends AbstractService {

    private enum ColumnType {DATE, INTEGER}

    private static final List<ColumnInfo> columns = new ArrayList<>();

    static {
        columns.add(new ColumnInfo("date", "`date`", ColumnType.DATE));
        columns.add(new ColumnInfo("distance", "distance", ColumnType.INTEGER));
        columns.add(new ColumnInfo("time", "time", ColumnType.INTEGER));
    }

    // Example
    // Input : (date eq '2016-05-01') AND ((distance gt 20) OR (distance lt 10))
    // Output: (`date` = '2016-05-01') AND ((distance > 20) OR (distance < 10))
    public String compileFilterToSQL(String filter) {
        if (StringUtils.isNullOrEmpty(filter)) return "";

        Parser.Node root = new Parser(filter).parse();
        SQLContext ctx = new SQLContext(columns);
        root.render(ctx);
        return ctx.toString();
    }
    
    private static class ColumnInfo {
        public final String name;
        public final String nameInDB;
        public final ColumnType type;

        public ColumnInfo(String name, String nameInDB, ColumnType type) {
            this.name = name;
            this.nameInDB = nameInDB;
            this.type = type;
        }
    }


    private static class SQLContext implements IRenderContext {
        private StringBuilder builder = new StringBuilder();
        private Map<String, ColumnInfo> columnsMap = new HashMap<>();

        public SQLContext(List<ColumnInfo> columns) {
            for (ColumnInfo column : columns) {
                columnsMap.put(column.name, column);
            }
        }

        @Override
        public void write(String str) {
            builder.append(str);
        }

        @Override
        public void comparison(String column, String cmpOperation, String value) {
            ColumnInfo info = ensureColumnExists(column);
            ensureValueHasValidType(info, value);
            builder.append(info.nameInDB)
                    .append(" ").append(convertOperation(cmpOperation)).append(" ")
                    .append(value);
        }

        private void ensureValueHasValidType(ColumnInfo info, String value) {
            switch (info.type) {
                case DATE:
                    try {
                        TimeUtils.parseDateTime_YYYYMMDD_GMT0(StringUtils.unquote(value));
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Column '"
                                + info.name + "' has type 'date', and expected format is 'YYYY-MM-DD'. Your actual value: " + value);
                    }
                    break;
                case INTEGER:
                    try {
                        Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        throw new RuntimeException("Column '" + info.name + "' has type 'int'. Your actual value: " + value);
                    }
                    break;
                default:
                    throw new RuntimeException("Unimplemented value type: " + info.type);
            }
        }

        private ColumnInfo ensureColumnExists(String column) {
            Assert.isTrue(columnsMap.containsKey(column), "Undefined column '" + column
                    + "'. Supported column names: "
                    + CollectionUtils.toCommaSeparatedList(new ArrayList<>(columnsMap.keySet())));
            return columnsMap.get(column);
        }

        private String convertOperation(String cmpOperation) {
            switch (cmpOperation.toLowerCase()) {
                case "gt":
                    return ">";
                case "lt":
                    return "<";
                case "ge":
                    return ">=";
                case "le":
                    return "<=";
                case "eq":
                    return "=";
                case "ne":
                    return "<>";
                default:
                    throw new RuntimeException("Undefined comparison operation: '" + cmpOperation + "'");
            }
        }

        @Override
        public String toString() {
            return builder.toString();
        }
    }
}
