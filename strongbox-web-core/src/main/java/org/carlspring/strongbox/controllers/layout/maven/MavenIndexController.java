package org.carlspring.strongbox.controllers.layout.maven;

import org.carlspring.strongbox.controllers.BaseController;
import org.carlspring.strongbox.providers.io.RepositoryPath;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.web.RepositoryMapping;

import javax.inject.Inject;
import java.io.IOException;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Kate Novik
 * @author carlspring
 */
@RestController
@Api(value = "/api/maven/index")
public class MavenIndexController
        extends BaseController
{

    private static final Logger logger = LoggerFactory.getLogger(MavenIndexController.class);

    @Inject
    private LocalIndexCreator localIndexCreator;


    @ApiOperation(value = "Used to create packed index for repository.")
    @ApiResponses(value = { @ApiResponse(code = 200, message = "The index was successfully generated"),
                            @ApiResponse(code = 500, message = "An error occurred."),
                            @ApiResponse(code = 404, message = "The specified (storageId/repositoryId/path) does not exist!") })
    @PreAuthorize("hasAuthority('MANAGEMENT_REBUILD_INDEXES')")
    @PostMapping(value = "package/{storageId}/{repositoryId}",
            produces = { MediaType.TEXT_PLAIN_VALUE,
                         MediaType.APPLICATION_JSON_VALUE })
    public ResponseEntity generatePackedRepositoryIndex(@RepositoryMapping Repository repository)
    {
        try
        {
            RepositoryPath indexPath = localIndexCreator.create(repository);

            return ResponseEntity.ok(
                    String.format("Packed index was generated in [%s].", indexPath));
        }
        catch (IOException e)
        {
            logger.error(e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body(e.getMessage());
        }
    }

}
