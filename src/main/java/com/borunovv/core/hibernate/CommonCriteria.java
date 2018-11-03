package com.borunovv.core.hibernate;

import org.hibernate.FetchMode;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projection;

public class CommonCriteria extends DetachedCriteria {

    private long offset = 0;
    private long count = 0;

    protected CommonCriteria(String entityName) {
        super(entityName);
    }

    protected CommonCriteria(String entityName, String alias) {
        super(entityName, alias);
    }

    public static CommonCriteria forClass(Class cl) {
        return new CommonCriteria(cl.getName());
    }

    public static CommonCriteria forClass(Class cl, String alias) {
        return new CommonCriteria(cl.getName(), alias);
    }

    public long getOffset() {
        return offset;
    }

    public CommonCriteria setOffset(long offset) {
        this.offset = offset;
        return this;
    }

    public long getCount() {
        return count;
    }

    public CommonCriteria setCount(long count) {
        this.count = count;
        return this;
    }

    @Override
    public CommonCriteria add(Criterion criterion){
        super.add(criterion);
        return this;
    }

    @Override
    public CommonCriteria addOrder(Order order){
        super.addOrder(order);
        return this;
    }

    @Override
    public CommonCriteria setFetchMode(String associationPath, FetchMode mode){
        super.setFetchMode(associationPath, mode);
        return this;
    }

    @Override
    public CommonCriteria createAlias(String associationPath, String alias){
        super.createAlias(associationPath, alias);
        return this;
    }

    @Override
    public CommonCriteria createAlias(String associationPath, String alias, int joinType){
        super.createAlias(associationPath, alias, joinType);
        return this;
    }

    @Override
    public CommonCriteria setProjection(Projection property){
        super.setProjection(property);
        return this;
    }
}