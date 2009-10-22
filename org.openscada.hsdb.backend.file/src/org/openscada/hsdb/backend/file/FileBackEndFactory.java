package org.openscada.hsdb.backend.file;

import java.io.File;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openscada.hsdb.StorageChannelMetaData;
import org.openscada.hsdb.backend.BackEnd;
import org.openscada.hsdb.backend.BackEndFactory;
import org.openscada.hsdb.backend.file.internal.DirectoryFileFilter;
import org.openscada.hsdb.backend.file.internal.FileFileFilter;
import org.openscada.hsdb.calculation.CalculationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class provides an implementation of the BackEndFactory for storage channel backend objects of type FileBackEnd.
 * @author Ludwig Straub
 */
public class FileBackEndFactory implements BackEndFactory
{
    /** The default logger. */
    private final static Logger logger = LoggerFactory.getLogger ( FileBackEndFactory.class );

    /** Text that is used to split the different parts of the generated file names. */
    private final static String FILENAME_PART_SEPERATOR = "_";

    /** File mask that is used when no other file mask is passed. Sample file: MyDataSource-AVG-1-1000000-1100000.wa (configurationId-calculationMethod-level-startTime-endTime) */
    public final static String FILE_MASK = "%1$s" + FILENAME_PART_SEPERATOR + "%3$s" + FILENAME_PART_SEPERATOR + "%2$s" + FILENAME_PART_SEPERATOR + "%4$s" + FILENAME_PART_SEPERATOR + "%5$s.va";

    /** Format string used to format time information. */
    private final static String TIME_FORMAT = "%1$04d%2$02d%3$02d.%4$02d%5$02d%6$02d.%7$03d";

    /** Regular expression for configuration id fragments. */
    private final static String CONFIGURATION_ID_REGEX_PATTERN = ".*";

    /** Regular expression for detail level id fragments. */
    private final static String DETAIL_LEVEL_ID_REGEX_PATTERN = "[-0-9]+";

    /** Regular expression for calculation method information fragments. */
    private final static String CALCULATION_METHOD_REGEX_PATTERN = ".*";

    /** Regular expression for start time fragments. */
    private final static String START_TIME_REGEX_PATTERN = "[-0-9.]+";

    /** Regular expression for end time fragments. */
    private final static String END_TIME_REGEX_PATTERN = "[-0-9.]+";

    /** Prepared empty backend array. */
    private final static BackEnd[] EMTPY_BACKEND_ARRAY = new BackEnd[0];

    /** Prepared empty metadata array. */
    private final static StorageChannelMetaData[] emptyMetaDataArray = new StorageChannelMetaData[0];

    /** Root folder within the storage files are located and new ones have to be created. */
    private final String fileRoot;

    /** Maximum detail level when the file connection should be keppt open to gain performance as long as the file back end instance stays initialized. */
    private final long maximumDetailLevelToKeepFileConnectionsOpen;

    /** Precompiled regular expression pattern for extracting the configuration id from a filename. */
    private final Pattern configurationIdPattern;

    /** Precompiled regular expression pattern for extracting the calculation method information from a filename. */
    private final Pattern calculationMethodPattern;

    /** Precompiled regular expression pattern for extracting the detail level id from a filename. */
    private final Pattern detailLevelIdPattern;

    /**
     * Constructor
     * @param fileRoot root folder within the storage files are located and new ones have to be created
     * @param maximumDetailLevelToKeepFileConnectionsOpen maximum detail level when the file connection should be keppt open to gain performance as long as the file back end instance stays initialized
     */
    public FileBackEndFactory ( final String fileRoot, final long maximumDetailLevelToKeepFileConnectionsOpen )
    {
        this.fileRoot = fileRoot;
        this.maximumDetailLevelToKeepFileConnectionsOpen = maximumDetailLevelToKeepFileConnectionsOpen;
        this.configurationIdPattern = Pattern.compile ( String.format ( FILE_MASK, "(" + CONFIGURATION_ID_REGEX_PATTERN + ")", CALCULATION_METHOD_REGEX_PATTERN, DETAIL_LEVEL_ID_REGEX_PATTERN, START_TIME_REGEX_PATTERN, END_TIME_REGEX_PATTERN ), Pattern.CASE_INSENSITIVE );
        this.calculationMethodPattern = Pattern.compile ( String.format ( FILE_MASK, CONFIGURATION_ID_REGEX_PATTERN, "(" + CALCULATION_METHOD_REGEX_PATTERN + ")", DETAIL_LEVEL_ID_REGEX_PATTERN, START_TIME_REGEX_PATTERN, END_TIME_REGEX_PATTERN ), Pattern.CASE_INSENSITIVE );
        this.detailLevelIdPattern = Pattern.compile ( String.format ( FILE_MASK, CONFIGURATION_ID_REGEX_PATTERN, CALCULATION_METHOD_REGEX_PATTERN, "(" + DETAIL_LEVEL_ID_REGEX_PATTERN + ")", START_TIME_REGEX_PATTERN, END_TIME_REGEX_PATTERN ), Pattern.CASE_INSENSITIVE );
    }

    /**
     * This method converts the time to a valid and readable part of a file name.
     * @param time time to be converted
     * @return converted time
     */
    private static String encodeFileNamePart ( final long time )
    {
        final Calendar calendar = Calendar.getInstance ();
        calendar.setTimeInMillis ( time );
        return String.format ( TIME_FORMAT, calendar.get ( Calendar.YEAR ), calendar.get ( Calendar.MONTH ), calendar.get ( Calendar.DAY_OF_MONTH ), calendar.get ( Calendar.HOUR_OF_DAY ), calendar.get ( Calendar.MINUTE ), calendar.get ( Calendar.SECOND ), calendar.get ( Calendar.MILLISECOND ) );
    }

    /**
     * This method converts the passed text to a valid part of a file name.
     * @param rawFileNamePart text to be converted
     * @return converted text
     */
    private static String encodeFileNamePart ( final String rawFileNamePart )
    {
        if ( rawFileNamePart == null )
        {
            return "";
        }
        try
        {
            return URLEncoder.encode ( rawFileNamePart, "utf-8" ).replaceAll ( FILENAME_PART_SEPERATOR, " " );
        }
        catch ( final Exception e )
        {
            return rawFileNamePart;
        }
    }

    /**
     * This method extracts data from the file name and returns the result.
     * If the desired information could not be extracted, then the default value will be returned instead.
     * @param pattern pattern that will be used to extract data from the filename
     * @param fileName filename from which data should be extracted
     * @param defaultValue default value that will be returned, if the desired information cannot be extracted from the filename
     * @return information extracted from the filename or default value if no information could be extracted
     */
    private static String extractDataFromFileName ( final Pattern pattern, final String fileName, final String defaultValue )
    {
        // check input
        if ( ( pattern == null ) || ( fileName == null ) )
        {
            return defaultValue;
        }

        // parse filename
        final Matcher matcher = pattern.matcher ( fileName );
        if ( !matcher.matches () || ( matcher.groupCount () != 1 ) )
        {
            return defaultValue;
        }
        final String result = matcher.group ( 1 );
        return result != null ? result : defaultValue;
    }

    /**
     * This method extracts data from the file name and returns the result.
     * If the desired information could not be extracted, then the default value will be returned instead.
     * @param pattern pattern that will be used to extract data from the filename
     * @param fileName filename from which data should be extracted
     * @param defaultValue default value that will be returned, if the desired information cannot be extracted from the filename
     * @return information extracted from the filename or default value if no information could be extracted
     */
    private static long extractDataFromFileName ( final Pattern pattern, final String fileName, final long defaultValue )
    {
        return Long.parseLong ( extractDataFromFileName ( pattern, fileName, "" + defaultValue ) );
    }

    /**
     * This method returns whether the file connection should be kept open for the specified detail level or not.
     * @param detailLevelId detail level for which the information should be provided
     * @return true, if the file connection should be kept open for the specified detail level, otherwise false
     */
    private boolean getKeepFileConnectionOpen ( final long detailLevelId )
    {
        return detailLevelId <= maximumDetailLevelToKeepFileConnectionsOpen;
    }

    /**
     * This method creates and initializes a back end object for the passed file object.
     * If the object is not used internally within this class, then the object should be deinitialized before passing the argument outside this class.
     * @param file file that is used to create a back end object
     * @param keepUpenWhileInitialized true, if the file connection should be kept open while the state of the instance is initialized, otherwise false
     * @return initialized back end object
     */
    private BackEnd getBackEnd ( final File file, final boolean keepUpenWhileInitialized )
    {
        FileBackEnd fileBackEnd = null;
        try
        {
            fileBackEnd = new FileBackEnd ( file.getPath (), keepUpenWhileInitialized );
            fileBackEnd.initialize ( null );
            final StorageChannelMetaData metaData = fileBackEnd.getMetaData ();
            final String fileName = file.getName ();
            final String configurationId = encodeFileNamePart ( metaData.getConfigurationId () );
            final String calculationMethod = CalculationMethod.convertCalculationMethodToShortString ( metaData.getCalculationMethod () );
            final long detailLevelId = metaData.getDetailLevelId ();
            if ( ( configurationId == null ) || !extractDataFromFileName ( configurationIdPattern, fileName, configurationId ).equals ( configurationId ) || ( !extractDataFromFileName ( calculationMethodPattern, fileName, calculationMethod ).equals ( calculationMethod ) ) || ( extractDataFromFileName ( detailLevelIdPattern, fileName, detailLevelId ) != detailLevelId ) )
            {
                fileBackEnd = null;
                logger.warn ( String.format ( "file content does not match expected content due to file name (%s). file will be ignored", file.getPath () ) );
            }
        }
        catch ( final Exception e )
        {
            fileBackEnd = null;
            logger.warn ( String.format ( "file '%s' could not be evaluated and will be ignored", file.getPath () ), e );
        }
        return fileBackEnd;
    }

    /**
     * This method returns the metadata objects of all existing back end objects within the specified directory.
     * If more than one metadata object exists for the same configuration, calculation
     * method and detail level then the additional information is merged into one single meta data object.
     * The time span is hereby widened so that the earliest start time is used and the latest end time.
     * All other information is taken from the sub meta data object with the latest end time.
     * @param directory directory in the root folder from which the back end files are loaded. if no directory is passed then all available directories within the root folder will be processed
     * @return metadata objects of all existing back end objects
     * @throws Exception in case of any problems
     */
    private StorageChannelMetaData[] getExistingBackEndsMetaDataInDirectory ( final String directory )
    {
        // check if root folder exists
        final File root = new File ( fileRoot );
        if ( !root.exists () || !root.isDirectory () )
        {
            return emptyMetaDataArray;
        }

        // get all directories
        final File[] directories = root.listFiles ( new DirectoryFileFilter ( directory ) );
        final List<StorageChannelMetaData> metaDatas = new LinkedList<StorageChannelMetaData> ();
        for ( final File configurationDirectory : directories )
        {
            for ( final File file : configurationDirectory.listFiles ( new FileFileFilter ( String.format ( FILE_MASK, configurationDirectory.getName (), CALCULATION_METHOD_REGEX_PATTERN, DETAIL_LEVEL_ID_REGEX_PATTERN, START_TIME_REGEX_PATTERN, END_TIME_REGEX_PATTERN ) ) ) )
            {
                final BackEnd backEnd = getBackEnd ( file, false );
                if ( backEnd != null )
                {
                    try
                    {
                        final StorageChannelMetaData metaData = backEnd.getMetaData ();
                        if ( metaData != null )
                        {
                            boolean addNew = true;
                            for ( final StorageChannelMetaData entry : metaDatas )
                            {
                                final String storedConfigurationId = entry.getConfigurationId ();
                                if ( ( storedConfigurationId != null ) && !storedConfigurationId.equals ( metaData.getConfigurationId () ) )
                                {
                                    // since the list is ordered by directory and therefore by configuration id, it can be assumed that no more suitable entry exists in the list
                                    break;
                                }
                                if ( ( entry.getDetailLevelId () == metaData.getDetailLevelId () ) && ( entry.getCalculationMethod () == metaData.getCalculationMethod () ) )
                                {
                                    // adapt the current entry in the list and expand the time span
                                    entry.setStartTime ( Math.min ( entry.getStartTime (), metaData.getStartTime () ) );
                                    final long endTime = metaData.getEndTime ();
                                    if ( entry.getEndTime () < endTime )
                                    {
                                        entry.setCalculationMethod ( metaData.getCalculationMethod () );
                                        entry.setCalculationMethodParameters ( metaData.getCalculationMethodParameters () );
                                        entry.setConfigurationId ( metaData.getConfigurationId () );
                                        entry.setDataType ( metaData.getDataType () );
                                        entry.setEndTime ( endTime );
                                        entry.setProposedDataAge ( metaData.getProposedDataAge () );
                                    }
                                    entry.setEndTime ( Math.max ( entry.getEndTime (), metaData.getEndTime () ) );
                                    addNew = false;
                                    break;
                                }
                            }
                            if ( addNew )
                            {
                                metaDatas.add ( 0, new StorageChannelMetaData ( metaData ) );
                            }
                        }
                        backEnd.deinitialize ();
                    }
                    catch ( final Exception e )
                    {
                        logger.warn ( String.format ( "metadata of file '%s' could not be retrieved. file will be ignored", file.getPath () ), e );
                    }
                }
            }
        }
        return metaDatas.toArray ( emptyMetaDataArray );
    }

    /**
     * @see org.openscada.hsdb.backend.BackEndFactory#getExistingBackEndsMetaData()
     */
    public StorageChannelMetaData[] getExistingBackEndsMetaData () throws Exception
    {
        return getExistingBackEndsMetaDataInDirectory ( null );
    }

    /**
     * @see org.openscada.hsdb.backend.BackEndFactory#getExistingBackEndsMetaData(String)
     */
    public StorageChannelMetaData[] getExistingBackEndsMetaData ( final String configurationId ) throws Exception
    {
        return getExistingBackEndsMetaDataInDirectory ( configurationId );
    }

    /**
     * @see org.openscada.hsdb.backend.BackEndFactory#getExistingBackEnds
     */
    public BackEnd[] getExistingBackEnds ( final String configurationId, final long detailLevelId, final CalculationMethod calculationMethod ) throws Exception
    {
        // check input
        if ( configurationId == null )
        {
            return EMTPY_BACKEND_ARRAY;
        }

        // check if root folder exists
        final File root = new File ( fileRoot );
        if ( !root.exists () || !root.isDirectory () )
        {
            return EMTPY_BACKEND_ARRAY;
        }

        // get all directories within the root folder
        final String configurationIdFileName = encodeFileNamePart ( configurationId );
        final File[] directories = root.listFiles ( new DirectoryFileFilter ( configurationIdFileName ) );

        // check if sub directory exists
        if ( directories.length == 0 )
        {
            return EMTPY_BACKEND_ARRAY;
        }

        // evaluate the configuration id directory
        final List<BackEnd> backEnds = new ArrayList<BackEnd> ();
        final boolean keepUpenWhileInitialized = getKeepFileConnectionOpen ( detailLevelId );
        for ( final File file : directories[0].listFiles ( new FileFileFilter ( String.format ( FILE_MASK, configurationIdFileName, CalculationMethod.convertCalculationMethodToShortString ( calculationMethod ), detailLevelId, START_TIME_REGEX_PATTERN, END_TIME_REGEX_PATTERN ) ) ) )
        {
            final BackEnd backEnd = getBackEnd ( file, keepUpenWhileInitialized );
            if ( backEnd != null )
            {
                backEnd.deinitialize ();
                backEnds.add ( backEnd );
            }
        }
        return backEnds.toArray ( EMTPY_BACKEND_ARRAY );
    }

    /**
     * @see org.openscada.hsdb.backend.BackEndFactory#createNewBackEnd
     */
    public BackEnd createNewBackEnd ( final StorageChannelMetaData storageChannelMetaData ) throws Exception
    {
        // check input
        if ( storageChannelMetaData == null )
        {
            final String message = "invalid StorageChannelMetaData object passed to FileBackEndFactory!";
            logger.error ( message );
            throw new Exception ( message );
        }
        final String configurationId = encodeFileNamePart ( storageChannelMetaData.getConfigurationId () );
        if ( configurationId == null )
        {
            final String message = "invalid configurationId specified as metadata for FileBackEndFactory!";
            logger.error ( message );
            throw new Exception ( message );
        }

        // assure that root folder exists
        return new FileBackEnd ( new File ( new File ( fileRoot, configurationId ), String.format ( FILE_MASK, configurationId, CalculationMethod.convertCalculationMethodToShortString ( storageChannelMetaData.getCalculationMethod () ), storageChannelMetaData.getDetailLevelId (), encodeFileNamePart ( storageChannelMetaData.getStartTime () ), encodeFileNamePart ( storageChannelMetaData.getEndTime () ) ) ).getPath (), getKeepFileConnectionOpen ( storageChannelMetaData.getDetailLevelId () ) );
    }

    /**
     * @see org.openscada.hsdb.backend.BackEndFactory#createNewBackEnd
     */
    public void deleteBackEnds ( final String configurationId )
    {
        // check input
        if ( configurationId == null )
        {
            return;
        }

        // check if root folder exists
        final File root = new File ( fileRoot );
        if ( !root.exists () || !root.isDirectory () )
        {
            return;
        }

        // get all directories within the root folder
        final String configurationIdFileName = encodeFileNamePart ( configurationId );
        final File[] directories = root.listFiles ( new DirectoryFileFilter ( configurationIdFileName ) );

        // check if sub directory exists
        if ( directories.length == 0 )
        {
            return;
        }

        // delete directories
        for ( final File directory : directories )
        {
            // delete existing back end files
            for ( final File file : directory.listFiles ( new FileFileFilter ( String.format ( FILE_MASK, configurationIdFileName, CALCULATION_METHOD_REGEX_PATTERN, DETAIL_LEVEL_ID_REGEX_PATTERN, START_TIME_REGEX_PATTERN, END_TIME_REGEX_PATTERN ) ) ) )
            {
                file.delete ();
            }

            // delete directory itself
            if ( !directory.delete () )
            {
                logger.warn ( String.format ( "could not delete directory '%s'", directory.getPath () ) );
            }
        }
    }
}
