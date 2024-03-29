package cn.bluejoe.elfinder.util;

import java.util.ArrayList;
import java.util.List;

import cn.bluejoe.elfinder.controller.executor.FsItemEx;
import cn.bluejoe.elfinder.service.FsItemFilter;
import sk.iway.iwcm.DB;

public abstract class FsItemFilterUtils
{
    public static FsItemFilter FILTER_ALL = new FsItemFilter()
    {
        @Override
        public boolean accepts(FsItemEx item)
        {
            return true;
        }
    };

    public static FsItemFilter FILTER_FOLDER = new FsItemFilter()
    {
        @Override
        public boolean accepts(FsItemEx item)
        {
            return item.isFolder();
        }
    };

    public static FsItemFilter createFileNameKeywordFilter(final String keyword)
    {
        return new FsItemFilter()
        {
            @Override
            public boolean accepts(FsItemEx item)
            {
                boolean accept = false;
                try
                {
                    if(item.getPath().toLowerCase().contains(keyword) || DB.internationalToEnglish(item.getName()).toLowerCase().contains(keyword))
                    {
                        accept = true;
                    }
                }
                catch (Exception exc)
                {
                    sk.iway.iwcm.Logger.error(exc);
                    return false;
                }

                return accept;
            }
        };
    }

    public static  List<FsItemEx> filterFiles(FsItemEx[] sourceFiles,
                                         FsItemFilter filter)
    {
        List<FsItemEx> filtered = new ArrayList<FsItemEx>();
        for (FsItemEx file : sourceFiles)
        {
            if (filter.accepts(file))
                filtered.add(file);
        }

        return filtered;//.toArray(new FsItemEx[0]);
    }

    /**
     * returns a FsItemFilter according to given mimeFilters
     *
     * @param mimeFilters
     *            An array of MIME types, if <code>null</code> no filtering is
     *            done
     * @return A filter that only accepts the supplied MIME types.
     */
    public static FsItemFilter createMimeFilter(final String[] mimeFilters)
    {
        if (mimeFilters == null || mimeFilters.length == 0)
            return FILTER_ALL;

        return new FsItemFilter()
        {
            @Override
            public boolean accepts(FsItemEx item)
            {
                String mimeType = item.getMimeType().toUpperCase();

                for (String mf : mimeFilters)
                {
                    mf = mf.toUpperCase();
                    if (mimeType.startsWith(mf + "/") || mimeType.equals(mf))
                        return true;
                }
                return false;
            }
        };
    }

}