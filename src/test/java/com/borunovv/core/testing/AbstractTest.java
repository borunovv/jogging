package com.borunovv.core.testing;

import com.borunovv.core.testing.AbstractTestWithFixture;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import javax.inject.Named;
import javax.sql.DataSource;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-context.xml"})
public abstract class AbstractTest extends AbstractTestWithFixture {
    @Override
    protected DataSource getDataSource() {
        return dataSource;
    }

    @Inject
    @Named("dataSource")
    private DataSource dataSource;
}
