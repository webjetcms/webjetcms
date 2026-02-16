package sk.iway.iwcm.system.jpa;

import java.util.List;

/**
 * Date: 28.02.2018
 * Time: 12:15
 * Project: webjet8
 * Company: InterWay a. s. (www.interway.sk)
 * Copyright: InterWay a. s. (c) 2001-2018
 *
 * @author mpijak
 */
public class PaginatedBean<T> {
    private int page;
    private int pageSize;
    private int total;
    private List<T> data;

    public int getTotalPages() {
        if (total % pageSize != 0) {
            return total / pageSize + 1;
        } else {
            return total / pageSize;
        }
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
