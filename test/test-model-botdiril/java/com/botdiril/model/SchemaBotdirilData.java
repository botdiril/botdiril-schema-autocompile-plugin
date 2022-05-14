package com.botdiril.model;

import java.time.LocalDateTime;

import com.botdiril.framework.sql.orm.ModelColumn;
import com.botdiril.framework.sql.orm.column.*;
import com.botdiril.framework.sql.orm.column.defaultvalue.DynamicDefaultValueSupplier;
import com.botdiril.framework.sql.orm.column.defaultvalue.ExpressionDefaultValueSupplier;
import com.botdiril.framework.sql.orm.schema.Schema;
import com.botdiril.framework.sql.orm.table.Table;

@Schema(name = "b50_data")
public class SchemaBotdirilData
{
    @Table(name = "object_names", prefix = "on")
    public static class TableObjectNames
    {
        @Column(dataType = long.class)
        @PrimaryKey
        @AutoIncrement
        public static ModelColumn<Long> id;

        @Column(dataType = long.class)
        @ForeignKey(value = TableObjectTypes.class, parentDeleteAction = ForeignKey.ParentDeleteAction.CASCADE_DELETE)
        @NotNull
        public static ModelColumn<Long> ot_id;

        @Column(dataType = String.class, bounds = 64)
        @NotNull
        public static ModelColumn<String> name;
    }

    @Table(name = "items", prefix = "it")
    public static class TableItems
    {
        @Column(dataType = long.class)
        @PrimaryKey
        @AutoIncrement
        public static ModelColumn<Long> id;

        @Column(dataType = long.class)
        @ForeignKey(value = TableObjectNames.class, parentDeleteAction = ForeignKey.ParentDeleteAction.CASCADE_DELETE)
        @NotNull
        public static ModelColumn<Long> on_id;
    }

    @Table(name = "object_types", prefix = "ot")
    public static class TableObjectTypes
    {
        @Column(dataType = long.class)
        @PrimaryKey
        @AutoIncrement
        public static ModelColumn<Long> id;

        @Column(dataType = String.class, bounds = 64)
        @NotNull
        @DefaultValue(value = ExpressionDefaultValueSupplier.class, expression = "'Joe'")
        public static ModelColumn<String> name;

        @Column(dataType = LocalDateTime.class)
        @NotNull
        @DefaultValue(DynamicDefaultValueSupplier.UTCTimestampNow.class)
        public static ModelColumn<LocalDateTime> time_created;
    }
}
