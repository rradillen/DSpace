/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.processor;

import org.apache.commons.lang.StringUtils;
import org.dspace.importer.external.metadatamapping.service.MetadataProcessorService;

/**
 * Removes the last point from an author name, this is required for the SAP lookup
 *
 * User: kevin (kevin at atmire.com)
 */
public class AuthorMetadataProcessorService implements MetadataProcessorService {

    /**
     * Strip a given value of its last dot (.)
     * @param value the value to run the processing over
     * @return The initial param with its ending dot stripped
     */
    @Override
    public String processMetadataValue(String value) {
        String ret=value;
        ret= StringUtils.strip(ret);
        ret= StringUtils.stripEnd(ret, ".");

        return ret;
    }
}
