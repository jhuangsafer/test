//SFONE-2
package com.safer.one.gwt.server;

import com.safer.data.struct.LastPassword;
import com.safer.data.struct.LastPasswordListType;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;
import javax.validation.Validator;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.PendingResult;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.grum.geocalc.EarthCalc;
import com.opencsv.CSVWriter;
import com.safer.Application;
import com.safer.Constants;
import com.safer.Version;
import com.safer.api.daq.providers.earthnetwork.EarthNetworkApiClient;
import com.safer.data.domain.Alarm;
import com.safer.data.domain.AlarmLevelsData;
import com.safer.data.domain.Daq;
import com.safer.data.domain.DefaultSensorAlarmValues;
import com.safer.data.domain.EmissionSource;
import com.safer.data.domain.ErgEvent;
import com.safer.data.domain.EventHistory;
import com.safer.data.domain.Feedback;
import com.safer.data.domain.KmlLayer;
import com.safer.data.domain.LightningAverage;
import com.safer.data.domain.ManualMetData;
import com.safer.data.domain.ManualSensor;
import com.safer.data.domain.ManualSensorAverage;
import com.safer.data.domain.MetAverage;
import com.safer.data.domain.MetData;
import com.safer.data.domain.MetReading;
import com.safer.data.domain.MetStation;
import com.safer.data.domain.Organization;
import com.safer.data.domain.PointOfInterest;
import com.safer.data.domain.Port;
import com.safer.data.domain.RaeMonitoringChemical;
import com.safer.data.domain.Scenario;
import com.safer.data.domain.ScenarioRun;
import com.safer.data.domain.Sensor;
import com.safer.data.domain.SensorAverage;
import com.safer.data.domain.SensorGroup;
import com.safer.data.domain.SensorInterface;
import com.safer.data.domain.SensorRae;
import com.safer.data.domain.Session;
import com.safer.data.domain.Setting;
import com.safer.data.domain.SharedEvent;
import com.safer.data.domain.Site;
import com.safer.data.domain.SiteSettings;
import com.safer.data.domain.TipOfTheDay;
import com.safer.data.domain.User;
import com.safer.data.domain.Zone;
import com.safer.data.domain.chemical.Chemical;
import com.safer.data.domain.chemical.ChemicalSite;
import com.safer.data.domain.chemical.ConcentrationIsopleths;
import com.safer.data.domain.chemical.MixtureChemical;
import com.safer.data.domain.sensor.SensorAveragesWrapper;
import com.safer.data.repository.DaqUpdatesRepository;
import com.safer.data.repository.chemical.ConcentrationIsoplethsRepository;
import com.safer.modeling.MetStationModelling;
import com.safer.modeling.ModelingManager;
import com.safer.modeling.SensorAverageModeling;
import com.safer.modeling.SensorModeling;
import com.safer.one.gwt.client.FieldValidationException;
import com.safer.one.gwt.client.SaferOneService;
import com.safer.one.gwt.client.report.ReportExtraOptionType;
import com.safer.one.gwt.client.report.ReportFileType;
import com.safer.one.gwt.client.tools.TIHTable;
import com.safer.one.gwt.client.ui.corridor.view.Corridor;
import com.safer.one.gwt.client.ui.tipoftheday.TipOfTheDayType;
import com.safer.one.gwt.shared.Config;
import com.safer.one.gwt.shared.CorridorOptions;
import com.safer.one.gwt.shared.DataSourceUtils;
import com.safer.one.gwt.shared.FormatUtils;
import com.safer.one.gwt.shared.GeometryUtils;
import com.safer.one.gwt.shared.InitialData;
import com.safer.one.gwt.shared.Labels;
import com.safer.one.gwt.shared.RegularUpdateData;
import com.safer.one.gwt.shared.ScenarioUtils;
import com.safer.one.gwt.shared.SensorUtils;
import com.safer.one.gwt.shared.Units;
import com.safer.one.gwt.shared.UserUtils;
import com.safer.one.gwt.shared.dto.AlarmDTO;
import com.safer.one.gwt.shared.dto.AlarmDtoI;
import com.safer.one.gwt.shared.dto.AutoSelectResponseDTO;
import com.safer.one.gwt.shared.dto.DaqDTO;
import com.safer.one.gwt.shared.dto.DefaultSensorAlarmValuesDTO;
import com.safer.one.gwt.shared.dto.DownwindDistanceDTO;
import com.safer.one.gwt.shared.dto.EarthNetworkStationDTO;
import com.safer.one.gwt.shared.dto.EditUserDTO;
import com.safer.one.gwt.shared.dto.EmissionSourceDTO;
import com.safer.one.gwt.shared.dto.EmissionSourceGasDTO;
import com.safer.one.gwt.shared.dto.EmissionSourceLiquidDTO;
import com.safer.one.gwt.shared.dto.EmissionSourceParticulateDTO;
import com.safer.one.gwt.shared.dto.EmissionSourcePipeDTO;
import com.safer.one.gwt.shared.dto.EmissionSourcePoolDTO;
import com.safer.one.gwt.shared.dto.EmissionSourcePuddleDTO;
import com.safer.one.gwt.shared.dto.EmissionSourceStackDTO;
import com.safer.one.gwt.shared.dto.EmissionSourceTankDTO;
import com.safer.one.gwt.shared.dto.ErgEventDTO;
import com.safer.one.gwt.shared.dto.EventHistoryDTO;
import com.safer.one.gwt.shared.dto.FeedbackDTO;
import com.safer.one.gwt.shared.dto.HasId;
import com.safer.one.gwt.shared.dto.ImpactedPoisCalculatedDTO;
import com.safer.one.gwt.shared.dto.KmlLayerDTO;
import com.safer.one.gwt.shared.dto.LightningAverageDTO;
import com.safer.one.gwt.shared.dto.LightningInfoDTO;
import com.safer.one.gwt.shared.dto.ManualMetDataDTO;
import com.safer.one.gwt.shared.dto.MetAverageDTO;
import com.safer.one.gwt.shared.dto.MetDataDTO;
import com.safer.one.gwt.shared.dto.MetStationDTO;
import com.safer.one.gwt.shared.dto.OrganizationDTO;
import com.safer.one.gwt.shared.dto.Pair;
import com.safer.one.gwt.shared.dto.PlumeDTO;
import com.safer.one.gwt.shared.dto.PointDTO;
import com.safer.one.gwt.shared.dto.PointOfInterestDTO;
import com.safer.one.gwt.shared.dto.PortDTO;
import com.safer.one.gwt.shared.dto.RTPointOfInterestDTO;
import com.safer.one.gwt.shared.dto.RainDataDTO;
import com.safer.one.gwt.shared.dto.SaferPointOfInterest;
import com.safer.one.gwt.shared.dto.ScenarioIsoplethDTO;
import com.safer.one.gwt.shared.dto.ScenarioMetDataDTO;
import com.safer.one.gwt.shared.dto.ScenarioOutDTO;
import com.safer.one.gwt.shared.dto.ScenarioPredefinedGridDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunFireballDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunGasReleaseDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunJetFireDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunLiquidReleaseDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunParticulateReleaseDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunPipeReleaseDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunPoolFireDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunPostDispersionGasExplosionDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunPostDispersionGasFireDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunPostDispersionLiquidExplosionDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunPostDispersionLiquidFireDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunPostDispersionPipeExplosionDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunPostDispersionPipeFireDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunPostDispersionTankExplosionDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunPostDispersionTankFireDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunPuddleReleaseDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunSensorInputDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunSolidExplosiveDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunStackReleaseDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunTankReleaseDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunTankTopFireDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunVaporCloudDTO;
import com.safer.one.gwt.shared.dto.ScenarioRunVesselBurstDTO;
import com.safer.one.gwt.shared.dto.SensorAlarmValuesDTO;
import com.safer.one.gwt.shared.dto.SensorAverageDTO;
import com.safer.one.gwt.shared.dto.SensorAveragesWrapperDTO;
import com.safer.one.gwt.shared.dto.SensorDTO;
import com.safer.one.gwt.shared.dto.SensorGroupDTO;
import com.safer.one.gwt.shared.dto.SensorHistoryDTO;
import com.safer.one.gwt.shared.dto.SensorInterfaceDTO;
import com.safer.one.gwt.shared.dto.SensorRaeDTO;
import com.safer.one.gwt.shared.dto.SensorRaeValue;
import com.safer.one.gwt.shared.dto.SettingDTO;
import com.safer.one.gwt.shared.dto.SharedEventDTO;
import com.safer.one.gwt.shared.dto.SiteDTO;
import com.safer.one.gwt.shared.dto.SiteSettingsDTO;
import com.safer.one.gwt.shared.dto.TestUnitDTO;
import com.safer.one.gwt.shared.dto.TipOfTheDayDTO;
import com.safer.one.gwt.shared.dto.UnitSettingWithGroupDTO;
import com.safer.one.gwt.shared.dto.UserDTO;
import com.safer.one.gwt.shared.dto.VerticalProfilePointDTO;
import com.safer.one.gwt.shared.dto.ZoneDTO;
import com.safer.one.gwt.shared.dto.ZonePointOfInterestDTO;
import com.safer.one.gwt.shared.dto.chemical.AntoineMixtureDetailsDTO;
import com.safer.one.gwt.shared.dto.chemical.ChemicalDTO;
import com.safer.one.gwt.shared.dto.chemical.ChemicalDetailsDTO;
import com.safer.one.gwt.shared.dto.chemical.ChemicalsWrapper;
import com.safer.one.gwt.shared.dto.chemical.ConcentrationIsoplethsDTO;
import com.safer.one.gwt.shared.dto.chemical.IsoplethConcentrationDTO;
import com.safer.one.gwt.shared.dto.chemical.MixtureChemicalDTO;
import com.safer.one.gwt.shared.dto.chemical.WilsonMixtureDetailsDTO;
import com.safer.one.gwt.shared.dto.erg.ErgChemicalIdDTO;
import com.safer.one.gwt.shared.dto.erg.ErgResults;
import com.safer.one.gwt.shared.dto.erg.ErgServerData;
import com.safer.one.gwt.shared.dto.utils.DaqUpdatesDTO;
import com.safer.one.gwt.shared.dto.utils.LightningHistoryChartDTO;
import com.safer.one.gwt.shared.dto.utils.LightningStrikesChartDTO;
import com.safer.one.gwt.shared.dto.utils.NewDataAvailableDTO;
import com.safer.one.gwt.shared.dto.utils.NewDataAvailableDTO.Status;
import com.safer.one.gwt.shared.dto.utils.PolygonDTO;
import com.safer.one.gwt.shared.dto.utils.SensorForValueComputationI;
import com.safer.one.gwt.shared.dto.utils.WindRoseDTO;
import com.safer.one.gwt.shared.enums.ChemicalType;
import com.safer.one.gwt.shared.enums.ConcentrationIsoplethsType;
import com.safer.one.gwt.shared.enums.EmissionSourceType;
import com.safer.one.gwt.shared.enums.ErgMetDataType;
import com.safer.one.gwt.shared.enums.ErgSpillTime;
import com.safer.one.gwt.shared.enums.ErgType;
import com.safer.one.gwt.shared.enums.LightningDanger;
import com.safer.one.gwt.shared.enums.MapLayerType;
import com.safer.one.gwt.shared.enums.MetAverageType;
import com.safer.one.gwt.shared.enums.MetStationType;
import com.safer.one.gwt.shared.enums.MixtureCompositionType;
import com.safer.one.gwt.shared.enums.MixtureModelType;
import com.safer.one.gwt.shared.enums.POIType;
import com.safer.one.gwt.shared.enums.RecordType;
import com.safer.one.gwt.shared.enums.Role;
import com.safer.one.gwt.shared.enums.ScenarioType;
import com.safer.one.gwt.shared.enums.SensorInterfaceType;
import com.safer.one.gwt.shared.enums.SensorType;
import com.safer.one.gwt.shared.enums.SettingParam;
import com.safer.one.gwt.shared.enums.TemplateType;
import com.safer.one.gwt.shared.enums.UnitDesc;
import com.safer.one.gwt.shared.enums.UnitParam;
import com.safer.one.gwt.shared.enums.VesselShape;
import com.safer.one.gwt.shared.enums.WindDirection;
import com.safer.one.gwt.shared.exception.EarthNetworkApiUnreachableException;
import com.safer.one.gwt.shared.exception.InvalidEarthNetworkCredentialsException;
import com.safer.one.gwt.shared.filter.DateStringFilterHandler;
import com.safer.one.gwt.shared.validation.AdminCreateUser;
import com.safer.one.gwt.shared.validation.AdminEditOtherUser;
import com.safer.one.gwt.shared.validation.EditMyProfile;
import com.safer.one.gwt.shared.validation.SaferAdminCreateUser;
import com.safer.one.loginmanager.server.LoginServiceImpl;
import com.safer.one.mapper.converter.PointConverter;
import com.safer.one.plume.Plume;
import com.safer.one.plume.PlumeRenderUtils;
import com.safer.one.plume.PlumeUtils;
import com.safer.one.plume.PuffCollect;
import com.safer.one.server.SaferContext;
import com.safer.one.service.AlarmManager;
import com.safer.one.service.ChemicalsManager;
import com.safer.one.service.DaqManagerI;
import com.safer.one.service.DataSourceManagerI;
import com.safer.one.service.EmissionManager;
import com.safer.one.service.EventHistoryManager;
import com.safer.one.service.EventManager;
import com.safer.one.service.FeedbackManager;
import com.safer.one.service.HttpServicesImpl;
import com.safer.one.service.KmlLayerManager;
import com.safer.one.service.OrganizationManager;
import com.safer.one.service.POIsManager;
import com.safer.one.service.ScenarioManager;
import com.safer.one.service.SettingManager;
import com.safer.one.service.SiteManagerI;
import com.safer.one.service.TipOfTheDayManager;
import com.safer.one.service.UnitsManagerImpl;
import com.safer.one.service.UserManagerI;
import com.safer.one.service.UserManagerImpl;
import com.safer.one.service.ZoneManager;
import com.safer.one.widgets.shared.FieldVerifier;
import com.safer.util.SalUtils;
import com.safer.util.chemical.ChemicalUtils;
import com.safer.util.email.EmailManager;
import com.safer.util.email.EmailManager.CallBack;
import com.safer.util.ergimporttools.ErgTihImport;
import com.safer.util.math.VectorUtils;
import com.safer.util.meteo.MetAverageProcessor;
import com.safer.util.meteo.MetCalculations;
import com.sencha.gxt.data.shared.SortDir;
import com.sencha.gxt.data.shared.SortInfo;
import com.sencha.gxt.data.shared.loader.FilterConfig;
import com.sencha.gxt.data.shared.loader.FilterPagingLoadConfig;
import com.sencha.gxt.data.shared.loader.ListLoadConfig;
import com.sencha.gxt.data.shared.loader.ListLoadResult;
import com.sencha.gxt.data.shared.loader.ListLoadResultBean;
import com.sencha.gxt.data.shared.loader.PagingLoadConfig;
import com.sencha.gxt.data.shared.loader.PagingLoadResult;
import com.sencha.gxt.data.shared.loader.PagingLoadResultBean;
import com.util.DateUtils;
import com.util.EncryptionUtils;
import com.util.IgnoreNullsTemplateExceptionHandler;
import com.util.SecureCredentials;
import com.util.ZipUtils;
import com.util.html.HtmlUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import fr.opensagres.xdocreport.core.document.SyntaxKind;
import fr.opensagres.xdocreport.document.IXDocReport;
import fr.opensagres.xdocreport.document.images.ByteArrayImageProvider;
import fr.opensagres.xdocreport.document.images.IImageProvider;
import fr.opensagres.xdocreport.document.registry.XDocReportRegistry;
import fr.opensagres.xdocreport.template.IContext;
import fr.opensagres.xdocreport.template.TemplateEngineKind;
import fr.opensagres.xdocreport.template.formatter.FieldsMetadata;
import fr.opensagres.xdocreport.template.freemarker.FreemarkerTemplateEngine;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModel;
import ma.glasnost.orika.MapperFacade;

@Component
@SuppressWarnings("serial")
public class SaferOneServiceImpl extends MyXsrfProtectedServiceServlet implements SaferOneService {
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	@Autowired
	private DaqUpdatesRepository daqUpdatesRepository;
	
	@Autowired
	LoginServiceImpl loginService;
	
	@Autowired
	private ApplicationContext ctx;

	@Autowired
	ModelingManager modelingManager;
	
	@Autowired
	ConcentrationIsoplethsRepository concIsoplethRepos;
	
	@Autowired
	private ChemicalsManager chemicalsManager;
	
	@Autowired
	private Validator validator;

	@Autowired
	private UserManagerI userManager;

	@Autowired
	private DaqManagerI daqManager;

	@Autowired
	private OrganizationManager organizationManager;
	
	@Autowired
	private TipOfTheDayManager tipOfTheDayManager;

	@Autowired
	private EmissionManager emissionManager;
	
	@Autowired
	private ScenarioManager scenarioManager;
	
	@Autowired
	AlarmManager alarmManager;
	
	@Autowired
	FeedbackManager feedbackManager;

	@Autowired
	SiteManagerI siteManager;

	@Autowired
	ZoneManager zoneManager;
	
	@Autowired
	DataSourceManagerI dataSourceManager;

	@Autowired
	MapperFacade mapper;

	@Autowired
	private Version version;

	@Autowired
	private UnitsManagerImpl unitsManager;

	@Autowired
	private SaferContext saferContext;

	@Autowired
	private POIsManager poisManager;

	@Autowired
	private EventManager eventManager;

	@Autowired
	private EventHistoryManager eventHistoryManager;
	
	@Autowired
	private Constants properties;

	@Autowired
	EarthNetworkApiClient earthNetworkApiClient;

	@Autowired
	private SettingManager settingManager;

	@Autowired
	private KmlLayerManager kmlLayerManager;
	
	@Value("classpath:templates/QuickTemplate.docx")
	private Resource quickTemplateResource;
	
	@Value("classpath:templates/ERGTemplate.docx")
	private Resource ergTemplateResource;
	
	@Value("classpath:templates/LightningPotentialTemplate.docx")
	private Resource lightningTemplateResource;
	
	@Value("classpath:templates/LightningStrikesTemplate.docx")
	private Resource lightningStrikesTemplateResource;
	
	@Value("classpath:templates/ScenarioTemplate.docx")
	private Resource scenarioTemplateResource;
	
	@Value("classpath:static/ergguide/Pdf")
	private Resource pdfGuidePath;
	
	@Autowired
	private Configuration freeMarkerConfiguration; 
	
	@Autowired
	private PdfConverterSaf pdfConverter;
	
	@Autowired
	private EmailManager emailManager;
	
	private MetStationDTO manualMetStationDto ;
//	
//	@Autowired
//	private ChemicalSiteRepository chemSiteRepos;
//
//	@Autowired
//	private MixtureChemicalRepository mixtureChemicalRepos;
	@PostConstruct
	private void postConstruct() {
	    manualMetStationDto = MetStation.createManualMetStation(null).asMetStationDTO(mapper);
	}
	
	private static final HashMap<SortDir, Sort.Direction> gxtToJpaSort = new HashMap<SortDir, Sort.Direction>();
	static {
		gxtToJpaSort.put(SortDir.ASC, Sort.Direction.ASC);
		gxtToJpaSort.put(SortDir.DESC, Sort.Direction.DESC);
	}

	@Override
	public void logOut() {
		logOut(true);
	}

	public void logOut(boolean removeSession) {
		if (getCurrentUser() != null) {
			if (removeSession) {
				removeSession();
			}
			((ScopedObject) saferContext).removeFromScope();
			getThreadLocalRequest().getSession().invalidate();
		}
	}
	private void removeSession() {
		Cookie[] cookies = getThreadLocalRequest().getCookies();
		if (cookies == null) {
			return;
		}
		for (Cookie cookie : cookies) {
			if ("saferRememberMe".equals(cookie.getName())) {

				Matcher cookieMatcher = HttpServicesImpl.REMEMBER_ME_COOKIE.matcher(cookie.getValue());

				if (cookieMatcher.matches()) {
					String seriesId = cookieMatcher.group(2);

					Session session = new Session();
					session.setUser(getCurrentUser());
					session.setSessionId(seriesId);
					userManager.deleteSession(session);
				}
			}
		}
	}

	private User getCurrentUser() {
		return saferContext.getCurrentUser();
	}
	
	private Site getCurrentSite() {
		return getCurrentUser().getCurrentSite();
	}
	
	@Override
	public String importChemicals(List<Integer> selectedIds,int siteID, int currentSiteID) {

		//String logFile=getServletContext().getRealPath("/uploadedFiles/chemical/"+String.valueOf(siteID)+".log");
		//String dataFile=getServletContext().getRealPath("/uploadedFiles/chemical/"+String.valueOf(siteID)+".json");
		String logFile=properties.getImportToolPath()+"/uploadedFiles/chemical/"+String.valueOf(siteID)+".log";
		String dataFile=properties.getImportToolPath()+"/uploadedFiles/chemical/"+String.valueOf(siteID)+".json";
		Site site = siteManager.getSite(siteID);
		//System.out.print("my currentSite:"+String.valueOf(currentSiteID));
		importChemical(dataFile,logFile,selectedIds,site);
		return "OK";
		
	}
	
	@Override
	public String importScenarios(List<Integer> selectedIds,int siteID, int currentSiteID) {

		//String logFile=getServletContext().getRealPath("/uploadedFiles/chemical/"+String.valueOf(siteID)+".log");
		//String dataFile=getServletContext().getRealPath("/uploadedFiles/chemical/"+String.valueOf(siteID)+".json");
		String logFile=properties.getImportToolPath()+"/uploadedFiles/scenario/"+String.valueOf(siteID)+".log";
		String dataFile=properties.getImportToolPath()+"/uploadedFiles/scenario/"+String.valueOf(siteID)+".json";
		Site site = siteManager.getSite(siteID);
		//System.out.print("my currentSite:"+String.valueOf(currentSiteID));
		importScenario(dataFile,logFile,selectedIds,site);
		return "OK";
		
	}	
	

	@Override
	public InitialData getInitialData() {

		InitialData initial = new InitialData();

		User currentUser = getCurrentUser();
		Site currentSite = currentUser.getCurrentSite();
		
		if (currentSite != null) {
			CorridorOptions corridorOptions = new CorridorOptions();
			corridorOptions.setColor("#FFFF00");
			corridorOptions.setWidth(4);
			corridorOptions.setPosition(new PointDTO(currentSite.getLatitude(), currentSite.getLongitude()));
			corridorOptions.setDownwind(true);

			initial.setCorridorOptions(corridorOptions);
			initial.setRegularUpdateData(getRegularUpdate(null, null));

			initial.setCurrentUser(mapper.map(currentUser, UserDTO.class));
			updateChangePasswordReason(initial.getCurrentUser());
			
			initial.setImpersonatingUserId(saferContext.getImpersonatingUserId());
			if(saferContext.getImpersonatingUserId()!=null) {
				initial.setImpersonatingUser(mapper.map(userManager.getUser(saferContext.getImpersonatingUserId()), UserDTO.class));
			}
			initial.setVersion(version.getVersion());
			initial.setUnits(unitsManager.getUnits(currentSite.getId(),getCurrentUser().getId()));
			SiteSettings siteSettings = siteManager.getSiteSettings(currentSite);
			initial.setSiteSettings(mapOrNewInstance(siteSettings, SiteSettingsDTO.class));						
			
			List<PointOfInterestDTO> pois = mapper.mapAsList(poisManager.getPoiRepository().findBySiteAndHiddenFalseAndGroupIdNull(currentSite),
					PointOfInterestDTO.class);
			HashMap<Integer, PointOfInterestDTO> m = new HashMap<Integer, PointOfInterestDTO>();
			for (PointOfInterestDTO p : pois) {
				m.put(p.getId(), p);
			}
			initial.setPois(m);

			List<EmissionSourceDTO> ems = mapper.mapAsList(emissionManager.getEmissionRepository().findBySiteAndHiddenFalseAndLocationNotNull(currentSite),
					EmissionSourceDTO.class);
			HashMap<Integer, EmissionSourceDTO> m1 = new HashMap<Integer, EmissionSourceDTO>();
			for (EmissionSourceDTO p : ems) {
				m1.put(p.getId(), p);
			}
			
			initial.setChemicals(new ChemicalsWrapper(chemicalsManager.findAll(getCurrentSite())));
			initial.setEmissionSources(m1);
			initial.setZones(getZones(null));
			initial.setMapLayers(getMapLayersSettings(currentUser));
			initial.setKmlLayers(getKmlLayersSettings(currentUser, currentSite));
			initial.setActiveProfile(Application.getActiveProfile());
			RaeMonitoringChemical raeChemical = siteManager.getRaeMonitoringChemical(currentSite, currentUser);
			if (raeChemical != null && raeChemical.getChemical() != null) {
				initial.setRaeMonitoringChemical(chemicalsManager.getChemicalDetails(raeChemical.getChemical().getId(), currentSite));
			}
			
		} else {
			initial.setError("You have no site enabled");
		}
		return initial;
	}

	private String getEarthNetworkKey() {
		SiteSettingsDTO settings = getSiteSettings(getCurrentSite().getId());
		if(StringUtils.isNotEmpty(settings.getEarthNetworkUsername())) {
			return settings.getEarthNetworkUsername();
		}
		return properties.getDefaultEarthNetworkUsername();
	}
	
	private void updateChangePasswordReason(UserDTO currentUserDTO) {
		if (getThreadLocalRequest().getSession(true).getAttribute(Labels.CHANGE_PASSWORD_REASON) != null) {
			currentUserDTO.setChangePasswordReason(getThreadLocalRequest().getSession().getAttribute(Labels.CHANGE_PASSWORD_REASON).toString());
			getThreadLocalRequest().getSession().removeAttribute(Labels.CHANGE_PASSWORD_REASON);
		} else if (getCurrentUser().getLastPasswordChange() != null) {
			
			Instant lastChange = getCurrentUser().getLastPasswordChange().toInstant();
			Instant now = Instant.now();
			if (now.minus(Config.PASSWORD_VALIDITY_DAYS, ChronoUnit.DAYS).isAfter(lastChange)) {
				currentUserDTO.setChangePasswordReason(Labels.PASSWORD_EXPIRED);
			} else {
				long warningDays = Config.PASSWORD_VALIDITY_DAYS-Config.WARN_BEFORE_PASSWORD_EXPIRE_DAYS;
				if (now.minus(warningDays, ChronoUnit.DAYS).isAfter(lastChange)){
					int daysSinceLastChange = (int) Math.floor(((now.toEpochMilli()-lastChange.toEpochMilli())/1000/60/60/24));
					int daysUntilExpiration = Config.PASSWORD_VALIDITY_DAYS-daysSinceLastChange;
					if (daysUntilExpiration <= 0) {//some toEpochMilli() differences might be converted to some strange number of days
						daysUntilExpiration = 1;
					}
					currentUserDTO.setChangePasswordReason(String.format(Labels.PASSWORD_EXPIRE_SOON, daysUntilExpiration));
				}
			}
		}
	}

	private List<MapLayerType> getMapLayersSettings(User user) {
		List<MapLayerType> layers = settingManager.getListSettings(SettingParam.MAP_LAYERS, user, null, new TypeReference<List<MapLayerType>>(){});
		if(layers==null) {
			layers = MapLayerType.getAllLayers();
			layers.remove(MapLayerType.TIME_RINGS);
		}
		return layers;
	}
	
	private List<Integer> getKmlLayersSettings(User user, Site site) {
		List<Integer> layers = settingManager.getListSettings(SettingParam.KML_LAYERS, user, site,  new TypeReference<List<Integer>>(){});
		if(layers == null) {
			layers = new ArrayList<Integer>();
		}
		return layers;
	}

//	private void dumpDetails() {
//		ObjectMapper mapper = new ObjectMapper();
//		int[] scenarioIds = new int[]{137, 138, 139, 140, 141, 142, 143, 144, 145};
//		try {
//		for (Integer id : scenarioIds) {
//			Scenario scen = scenarioManager.getScenario(id);
//			mapper.writerWithDefaultPrettyPrinter().writeValue(new File("c:\\work\\safer\\json import\\scenarios\\" + scen.getName() + ".json"), scen);
//		}
//
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}
	/**
	 * If date is present we will get the average type based on date mod 5 minutes ({@link DataSourceUtils#getWeatherHistoryAverageType(Date)}
	 * @param date - the date locked in client with weather history
	 * @param metStationId - the metstation used in weather history
	 */
	@Override
	public RegularUpdateData getRegularUpdate(Date date, Integer metStationId) {
		
		User currentUser = getCurrentUser();
		Site currentSite = getCurrentUser().getCurrentSite();
		SiteSettings siteSettings = siteManager.getSiteSettings(currentSite);
		if (date != null) {
			date = DateUtils.convertFromTimezoneToUtc(date, currentSite.getTimeZone());
		}
				
		RegularUpdateData regular = new RegularUpdateData();

		Site currentSiteDb = siteManager.getSite(currentSite.getId());
		if(Boolean.FALSE.equals(currentSiteDb.getEnabled())) {
			currentSite.setEnabled(false);
			regular.setError("Your current site was disabled");
			return regular;
		}
		
		Map<Integer, MetStationDTO> metStations = new HashMap<Integer, MetStationDTO>();
		Map<MetAverageType, Map<Integer, MetAverageDTO>> metAveragesByType = new HashMap<>();
		
		metStations.put(MetStationDTO.MANUAL_MET_STATION_ID, manualMetStationDto);
		 
		MetStation primaryMetStation = dataSourceManager.getPrimaryMetStation(currentUser, currentSite);
		if (primaryMetStation != null) {
			regular.setPrimaryMetStation(primaryMetStation.asMetStationDTO(mapper));
		}else {
			regular.setPrimaryMetStation(manualMetStationDto);
		}

		getRegularUpdateMetAverages(date, metStations, metAveragesByType, 
			 date == null ? getSiteSettings(null).getMetAverageType():DataSourceUtils.getWeatherHistoryAverageType(date),
			 isRegularUpdateYellowLineAverage(date));
		
		MetStation weatherHistoryMet = null;
		if (metStationId != null) {
			weatherHistoryMet = dataSourceManager.getMetStation(metStationId);
		}
		if (weatherHistoryMet != null) {
			MetAverage latestWeatherHistoryAverage = dataSourceManager.getLastMetAverageOrInstant(weatherHistoryMet, MetAverageType.ONE_MINUTE_AVERAGE);
			if (latestWeatherHistoryAverage != null) {
				latestWeatherHistoryAverage.setDateTaken(DateUtils.convertToTimezone(latestWeatherHistoryAverage.getDateTaken(), currentSite.getTimeZone()));
				regular.setLatestWeatherHistoryAverage(latestWeatherHistoryAverage.asMetAverageDTO(mapper));
			}
		}
		List<MetStation> disabledMetStations = dataSourceManager.getDisabledMetStations(currentSite);
		Map<Integer, MetStationDTO> disabledMetStationsDtos = new HashMap<Integer, MetStationDTO>();
		for (MetStation met : disabledMetStations) {
			MetStationDTO metDTO = met.asMetStationDTO(mapper);
			disabledMetStationsDtos.put(met.getId(), metDTO);
		}
		

		List<Alarm> alarms = alarmManager.getAlarms(currentSite);
		List<AlarmDTO> alarmDTOs = mapper.mapAsList(alarms, AlarmDTO.class);
		for(AlarmDTO alarm : alarmDTOs) {
			alarm.setStartDate(DateUtils.convertToTimezone(alarm.getStartDate(), currentSite.getTimeZone()));
			alarm.setLastDate(DateUtils.convertToTimezone(alarm.getLastDate(), currentSite.getTimeZone()));
			alarm.setAcknowledgedBy(null); //todo mihai: so we do not carry also the userDTO. we don't need it here. Maybe make another DTO.
		}
		
		List<SharedEvent> sharedEvents = eventManager.getSharedEvents(currentUser);
		List<SharedEventDTO> sharedEventsDTOs = new ArrayList<SharedEventDTO>();
		sharedEventsDTOs = mapper.mapAsList(sharedEvents, SharedEventDTO.class);
		for(SharedEventDTO sharedEvent : sharedEventsDTOs ) {
			EventHistoryDTO eventHistory = sharedEvent.getEventHistory();
			if(eventHistory != null) {
				eventHistory.setCreatedDate(DateUtils.convertToTimezone(eventHistory.getCreatedDate() , currentSite.getTimeZone()));
				eventHistory.setReleaseTime(DateUtils.convertToTimezone(eventHistory.getReleaseTime() , currentSite.getTimeZone()));
			}
		}
		

		Map<Integer, List<Integer>> raeSensorIds = new HashMap<>();
		Map<Integer, List<Integer>> sensorGroupIds = new HashMap<>();
		
		List<Sensor> sensors;
		if (date == null) {
			if (siteSettings.isLightning()) {
				sensors = dataSourceManager.getSensorsEnabled(currentSite);
			} else {
				sensors = dataSourceManager.getNonLightningSensorsEnabled(currentSite);
			}
		} else {
			sensors = dataSourceManager.getSensorsThatHaveAverages(currentSite.getId(), date);
		}
		List<SensorDTO> sensorsList = new ArrayList<SensorDTO>();
		
		SensorAveragesWrapper averagesWrapper = new SensorAveragesWrapper();
		averagesWrapper.setSensors(sensors);
		averagesWrapper.setSensorAverages(new ArrayList<SensorAverage>());
		
		RaeMonitoringChemical raeMonitoringChemical = siteManager.getRaeMonitoringChemical(currentSite, currentUser);
		Chemical raeChemical = null;
		if (raeMonitoringChemical != null) {
			raeChemical = raeMonitoringChemical.getChemical();
		}
		
		List<Sensor> lightningStrikesSensors = new ArrayList<>();
		List<Sensor> lightningPotentialSensors = new ArrayList<>();
		List<Sensor> lightningSensors = new ArrayList<>();
		Map<Integer, SensorAverageDTO> senAvg = new HashMap<Integer, SensorAverageDTO>();
		Map<Integer, LightningAverageDTO> lightningAvgs = new HashMap<Integer, LightningAverageDTO>();
		
		for (Sensor s:sensors) {
			SensorAverage avg;
			if(siteSettings.isLightning() && s.getType().isLightning()) {
				if(SensorType.LIGHTNING_POTENTIAL.equals(s.getType())) {
					lightningPotentialSensors.add(s);
				}
				if(SensorType.LIGHTNING_STRIKES.equals(s.getType())) {
					lightningStrikesSensors.add(s);
				}
				lightningSensors.add(s);
				LightningAverage lightningAvg;
				if (date == null) {
					lightningAvg = dataSourceManager.getLastLightningAverage(s);
				} else {
					lightningAvg = dataSourceManager.getLightningAverage(date, s);
				}	
				if(lightningAvg!=null) {
					lightningAvg.setDateTaken(DateUtils.convertToTimezone(lightningAvg.getDateTaken(), currentSite.getTimeZone()));
					lightningAvgs.put(s.getId(), mapper.map(lightningAvg,LightningAverageDTO.class));
				}
			}
			if (date == null) {
				avg = dataSourceManager.getLastSensorAverage(s);
			} else {
				avg = dataSourceManager.getSensorAverage(date, s);
			}
			
			if (avg != null) {
				if (raeChemical == null) {
					avg.setDateTaken(DateUtils.convertToTimezone(avg.getDateTaken(), currentSite.getTimeZone()));
					senAvg.put(s.getId(), mapper.map(avg, SensorAverageDTO.class));
				} else{
					averagesWrapper.getSensorAverages().add(avg);
				}
			}
		}
		
		regular.setLightningAverages(lightningAvgs);
		
		if (raeChemical == null) {
			regular.setSensorAverages(senAvg);
		} else {
			SensorAveragesWrapperDTO valuesWrapper = getSensorComputedValues(false, true, currentSite, raeChemical, averagesWrapper);
			regular.setSensorAverages(valuesWrapper.getSensorAverages());
			senAvg = valuesWrapper.getSensorAverages();
		}
		
		Map<Integer, SensorDTO> sensorsMap = new HashMap<Integer, SensorDTO>();
		for (Sensor s:sensors) {
			if (date == null || senAvg.containsKey(s.getId())) {//when we fetch the data for weather history locked dates, we only display the sensors with values
				SensorDTO sDTO = mapper.map(s, SensorDTO.class);
				sensorsList.add(sDTO);
				sensorsMap.put(s.getId(), sDTO);
				if(s.getSensorRae()!=null) {
					Integer raeId = s.getSensorRae().getId();
					if(!raeSensorIds.containsKey(raeId)) {
						raeSensorIds.put(raeId,new ArrayList<>());
					}
					raeSensorIds.get(raeId).add(s.getId());
				}
				if(s.getSensorGroup()!=null) {
					Integer groupId = s.getSensorGroup().getId();
					if(!sensorGroupIds.containsKey(groupId)) {
						sensorGroupIds.put(groupId,new ArrayList<>());
					}
					sensorGroupIds.get(groupId).add(s.getId());
				}
			}
		}
		regular.setSensors(sensorsMap);
		regular.setRaeSensorIds(raeSensorIds);
		regular.setSensorGroupIds(sensorGroupIds);
		
		List<SensorRae> sensorRaes;
		if (date == null) {
			sensorRaes = dataSourceManager.getSensorsRaeEnabled(currentSite);
		} else {
			sensorRaes = dataSourceManager.getSensorsRae(currentSite);
		}
		
		Map<Integer, SensorRaeDTO> sensorRaesMap = new HashMap<Integer, SensorRaeDTO>();
		for (SensorRae s:sensorRaes) {
			if (date == null || raeSensorIds.containsKey(s.getId())) {
				SensorRaeDTO raeDTO = mapper.map(s, SensorRaeDTO.class);
				if (date != null) {
					raeDTO.setLocation(senAvg.get(raeSensorIds.get(s.getId()).get(0)).getLocation());
				}
				sensorRaesMap.put(s.getId(), raeDTO);
			}
		}
		regular.setSensorRaes(sensorRaesMap);
		
		List<SensorGroup> sensorGroups;
		if (date == null) {
			sensorGroups = dataSourceManager.getSensorGroupsEnabled(currentSite);
		} else {
			sensorGroups = dataSourceManager.getSensorGroups(currentSite);
		}
		
		Map<Integer, SensorGroupDTO> sensorGroupMap = new HashMap<Integer, SensorGroupDTO>();
		for (SensorGroup s:sensorGroups) {
			if (date == null || sensorGroupIds.containsKey(s.getId())) {
				SensorGroupDTO groupDTO = mapper.map(s, SensorGroupDTO.class);
				if (date != null) {
					groupDTO.setLocation(senAvg.get(sensorGroupIds.get(s.getId()).get(0)).getLocation());
				}
				sensorGroupMap.put(s.getId(), groupDTO);
			}
		}
		regular.setSensorGroups(sensorGroupMap);
		
		
		if(siteSettings.isLightning() && !lightningPotentialSensors.isEmpty() || !lightningStrikesSensors.isEmpty()) {
			LightningInfoDTO lightningInfo = new LightningInfoDTO();
			Date time = null;
			
		
			if(date==null) {
				Date lastPotential = getLastSensorAverageDate(lightningPotentialSensors, senAvg);
				Date lastStrikes = getLastSensorAverageDate(lightningStrikesSensors, senAvg);
				
				if(lastPotential == null) {
					time = lastStrikes;
				}else if(lastStrikes == null) {
					time = lastPotential;
				}else if(lastPotential.before(lastStrikes)) {
					time = lastStrikes;
				}else {
					time = lastPotential;
				}
			}else {
				time = DateUtils.convertToTimezone(date, currentSite.getTimeZone());
			}
			lightningInfo.setTime(time);


			for(Sensor sensor : lightningSensors) {
				if(SensorType.LIGHTNING_POTENTIAL.equals(sensor.getType())) {
					lightningInfo.getPotentialSensorIds().add(sensor.getId());	
				}
				if(SensorType.LIGHTNING_STRIKES.equals(sensor.getType())) {
					lightningInfo.getStrikeSensorIds().add(sensor.getId());
				}

				if(lightningAvgs.get(sensor.getId())!=null) {
					LightningAverageDTO avg = lightningAvgs.get(sensor.getId());
					if(time.getTime() == avg.getDateTaken().getTime()) {
						Double statusPotential = avg.getStatusPotential() != null ? avg.getStatusPotential() : 0;
						Double statusStrikes = avg.getStatusStrikes() != null ? avg.getStatusStrikes() : 0;
						Double status = Math.max(statusPotential, statusStrikes);

						if(lightningInfo.getDanger() == null) {
							lightningInfo.setDanger(LightningDanger.NORMAL);
							lightningInfo.setDangerSensorId(sensor.getId());
						}
						if(status == 2  && LightningDanger.NORMAL.equals(lightningInfo.getDanger())) {
							lightningInfo.setDanger(LightningDanger.WARNING);
							lightningInfo.setDangerSensorId(sensor.getId());
						}
						if(status == 3) {
							lightningInfo.setDanger(LightningDanger.DANGER);
							lightningInfo.setDangerSensorId(sensor.getId());
						}
					}
				}
			}
			LightningAverage lastLightning = dataSourceManager.getLastLightning(lightningStrikesSensors, date != null ? date : new Date() , false);
			LightningAverage lastLightningFiveMiles = dataSourceManager.getLastLightning(lightningStrikesSensors, date != null ? date : new Date() , true);
			if(lastLightning!=null) {
				lightningInfo.setLastStrike(DateUtils.convertToTimezone(lastLightning.getDateTaken(), currentSite.getTimeZone()));
				lightningInfo.setLastStrikeSensorId(lastLightning.getSensor().getId());
			}
			if(lastLightningFiveMiles!=null) {
				lightningInfo.setLastStrikeFiveMiles(DateUtils.convertToTimezone(lastLightningFiveMiles.getDateTaken(), currentSite.getTimeZone()));
			}
			
			regular.setLightningInfo(lightningInfo);
		}else {
			regular.setLightningInfo(null);
		}
		
		
		regular.setAlarms(alarmDTOs);
		regular.setUserSettings(getUserSettings());
		regular.setMetStations(metStations);
		regular.setDisabledMetStations(disabledMetStationsDtos);
		regular.setMetAveragesByType(metAveragesByType);
		regular.setSharedEvents(sharedEventsDTOs);
		Date now = DateUtils.trimToSeconds(new Date());
		
		regular.setCurrentTime(DateUtils.convertToTimezone(now, currentSite.getTimeZone()));
		regular.setCurrentTimeRoundedToMinute(DateUtils.trimToMinutes(regular.getCurrentTime()));
		regular.setSelectedTime(DateUtils.convertToTimezone(date, currentSite.getTimeZone()));
		regular.setCurrentTimezoneOffset(TimeZone.getTimeZone(currentSite.getTimeZone()).getOffset(now.getTime()));
		regular.setCurrentUser(mapper.map(currentUser, UserDTO.class));
		
		return regular;
	}
	
	private Date getLastSensorAverageDate(List<Sensor> sensors, Map<Integer,SensorAverageDTO> senAvg) {
		Date last = null;
		for(Sensor sensor : sensors) {
			
			if(senAvg.get(sensor.getId())!=null) {
				SensorAverageDTO avg = senAvg.get(sensor.getId());
				Double value = avg.getValue();
				if(value!=null) {
					if(last == null || avg.getDateTaken().after(last)) {
						last = avg.getDateTaken();
					}
				}
			}
		}
		return last;
	}
	
	@Override
	public NewDataAvailableDTO isNewDataAvailable(int scenarioRunId, ScenarioType scenType) {
		if(ScenarioUtils.licenseDoesNotAlow(scenType, siteManager.getSiteSettings(getCurrentSite()))){
			return new NewDataAvailableDTO(Status.STOP_CHECKING, scenarioRunId);
		}
		return scenarioManager.isNewDataAvailable(scenarioRunId, getCurrentSite().getId());
	}
	
	/**
	 * compute yellow line when date is after the latest minute, multiple of 5, plus 1 minute, relative to selected date. 
	 * we do not compute yellow line with only one average, since it will be the same result as a normal call (that's why I add 1 minute to the recentFiveMultipleMinute).
	 * <br><i>This is different then the yellow line in weather history. In weather history I compute the average even for only one minute and it's computed relative to current time</i> 
 	 * @param date
	 * @return
	 */
	//Has test
	public boolean isRegularUpdateYellowLineAverage(Date date) {
		return date != null && 
				new Date((DateUtils.getLatestRelativeMinuteMultipleOfFive(date).getTime()+60*1000)).before(date);
	}

	/**
	 * 
	 * @param date
	 * @param metStationId
	 * @param metStations
	 * @param metAveragesByType
	 * @param metType
	 * @param partialAverages - when we have locked date in weather history and date is not multiple of 5 minute, 
	 * 	we want the average to be the average of the 1 min averages since the latest minute multiple of 5 minutes, relative to current time (yellow line) 
	 */
	public void getRegularUpdateMetAverages(Date date, 
		/*out*/ Map<Integer, MetStationDTO> metStations,
		/*out*/ Map<MetAverageType, Map<Integer, MetAverageDTO>> metAveragesByType,
		MetAverageType metType, boolean partialAverages) {
	    List<MetStation> siteMetStations = dataSourceManager.getMetStations(getCurrentSite());

	    Map<Integer, MetAverageDTO> metAverages = new HashMap<Integer, MetAverageDTO>();
	    metAveragesByType.put(metType, metAverages);

	    metAverages.put(-1, dataSourceManager.getManualMetAverage(getCurrentUser(), getCurrentSite()));

	    for (MetStation met : siteMetStations) {
		MetStationDTO metDTO = met.asMetStationDTO(mapper);
		if (metStations != null) {
		    metStations.put(met.getId(), metDTO);
		}

		MetAverage metAverage = null;
		if (partialAverages) {
		    metAverage = getYellowLineAverage(met, date);
		} else if (date != null) {
		    metAverage = dataSourceManager.getMetAverage(date, met, metType);
		}

		if(metAverage==null){
		    metAverage = dataSourceManager.getLastMetAverageOrInstant(met, metType);
		}

		if (metAverage != null) {
		    MetAverageDTO metAverageDTO = metAverage.asMetAverageDTO(mapper);
		    metAverageDTO.setDateTaken(DateUtils.convertToTimezone(metAverageDTO.getDateTaken(), getCurrentSite().getTimeZone()));
		    metAverages.put(met.getId(), metAverageDTO);
		}
	    }


	}
	
	@Override
	public Map<SettingParam, SettingDTO> getUserSettings() {
		User currentUser = getCurrentUser();
		List<Setting> settings = settingManager.getSettings(currentUser);
		Map<SettingParam, SettingDTO> userSettings = new HashMap<SettingParam, SettingDTO>();
		for (Setting setting : settings) {
			userSettings.put(SettingParam.valueOf(setting.getKey()), mapper.map(setting, SettingDTO.class));
		}
		return userSettings;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <T extends HasId> PagingLoadResult<T> getGridData(FilterPagingLoadConfig config, RecordType gridType) {
		Page page = null;
		List dto = null;
		PageRequest pageable;

		if (config.getSortInfo().size() == 1) {
			SortInfo sortInfo = config.getSortInfo().get(0);
			pageable = new PageRequest(config.getOffset() / config.getLimit(), config.getLimit(), gxtToJpaSort.get(sortInfo.getSortDir()),
					sortInfo.getSortField());
		} else {
			pageable = new PageRequest(config.getOffset() / config.getLimit(), config.getLimit());
		}
		
		String filterString = null;
		if (config.getFilters() != null) {
				for (FilterConfig filter: config.getFilters()){
					switch (filter.getField()) {
				case Labels.GRID_FILTER:
					filterString = filter.getValue();
					break;
				default:
					break;
				}
			}
		}
		
		switch (gridType) {
		case USER:
			Integer organizationId = null;
			if (config.getFilters() != null) {
				for (FilterConfig filter: config.getFilters()){
					switch (filter.getField()) {
					case "organization":
						organizationId = Integer.parseInt(filter.getValue());
						break;
					default:
						break;
					}
				}
			}
			page = getGridPageUser(pageable, organizationId, filterString);
			dto = mapper.mapAsList(page.getContent(), UserDTO.class);
			break;
		case ORGANIZATION:
			page = getGridPageOrganizations(config, pageable, filterString);
			dto = mapper.mapAsList(page.getContent(), OrganizationDTO.class);
			break;
		case TIPOFTHEDAY:
			page = getGridPageTipOfTheDay(config, pageable);
			dto = mapper.mapAsList(page.getContent(), TipOfTheDayDTO.class);
			break;
		case DAQ:
			if(UserUtils.isSiteAdminOnly(getCurrentUser().getRole())) {
				page = daqManager.getDaqs(getCurrentUser().getSites(), pageable);
			}else if(getCurrentUser().isOrganizationAdmin()){
				page = daqManager.getDaqs(getCurrentUser().getOrganization(), pageable);
			}
			dto = mapper.mapAsList(page.getContent(), DaqDTO.class);
			if (!UserUtils.isSaferAdmin(getCurrentUser().getRole())) {
				for (Object o:dto) {
					DaqDTO d = (DaqDTO)o;
					if (Boolean.TRUE.equals(d.getDefaultDaq())){
						//hide the default daq key for non super admins
						d.setDaqKey(null);
					}
				}
			}
			break;
		case ALARM: {
				Date before = null;
	 			Date after = null;
				String tz = getCurrentSite().getTimeZone();
				if (config.getFilters() != null) {
	  				for (FilterConfig filter: config.getFilters()){
	  					Date date = DateUtils.convertFromTimezoneToUtc(new DateStringFilterHandler().convertToObject(filter.getValue()), tz);
	 					switch (filter.getField()) {
						case "startDate":
							if("after".equals(filter.getComparison())) {
								after = date;
							}
							if("before".equals(filter.getComparison())) {
								before = date;
							}
							if("on".equals(filter.getComparison())) {
								after = date;
								before = DateUtils.add(date, Calendar.DAY_OF_YEAR, 1);
							}
							break;
						default:
							break;
						}
					}
				}
				
				page = alarmManager.getAlarmHistory(getCurrentUser().getCurrentSite(), before, after, pageable);
				dto = mapper.mapAsList(page.getContent(),  AlarmDTO.class);
				if(dto!=null){
					for(Object ob:dto){
						AlarmDTO al = (AlarmDTO)ob;
						al.setStartDate(DateUtils.convertToTimezone(al.getStartDate(), getCurrentSite().getTimeZone()));
						al.setAcknowledgedAt(DateUtils.convertToTimezone(al.getAcknowledgedAt(),getCurrentSite().getTimeZone()));
						al.setLastDate(DateUtils.convertToTimezone(al.getLastDate(), getCurrentSite().getTimeZone()));
					}
				}
			}
			break;
		case FEEDBACK: {
				Date before = null;
	 			Date after = null;
				String tz = getCurrentSite().getTimeZone();
				if (config.getFilters() != null) {
	  				for (FilterConfig filter: config.getFilters()){
	  					Date date = DateUtils.convertFromTimezoneToUtc(new DateStringFilterHandler().convertToObject(filter.getValue()), tz);
	 					switch (filter.getField()) {
						case "startDate":
							if("after".equals(filter.getComparison())) {
								after = date;
							}
							if("before".equals(filter.getComparison())) {
								before = date;
							}
							if("on".equals(filter.getComparison())) {
								after = date;
								before = DateUtils.add(date, Calendar.DAY_OF_YEAR, 1);
							}
							break;
						default:
							break;
						}
					}
				}
				
				page = feedbackManager.getFeedbackHistory(before, after, pageable);
				dto = mapper.mapAsList(page.getContent(),  FeedbackDTO.class);
				if(dto!=null){
					for(Object ob:dto){
						FeedbackDTO al = (FeedbackDTO)ob;
						al.setCreatedDate(DateUtils.convertToTimezone(al.getCreatedDate(), getCurrentSite().getTimeZone()));
					}
				}
			}
			break;
		case SITE:
			page = getGridPageSite(pageable, filterString);
			dto = mapper.mapAsList(page.getContent(), SiteDTO.class);
			break;
		case KML_LAYER:
			page = kmlLayerManager.getKmlLayers(getCurrentSite(),pageable);
			dto = mapper.mapAsList(page.getContent(), KmlLayerDTO.class);
			break;
		case EVENT:
			page = eventHistoryManager.getGridPageEvent(getCurrentSite(), filterString, pageable);
			dto = mapper.mapAsList(page.getContent(), EventHistoryDTO.class);
			break;
		case WEATHER_HISTORY:
			Date date = null;
			Integer metStationId = null;
			if (config.getFilters() != null) {
				for (FilterConfig filter: config.getFilters()){
					switch (filter.getField()) {
					case "date":
						DateTimeFormatter dtf = DateTimeFormat.forPattern(Config.WEATHER_HISTORY_FORMAT).withZone(DateTimeZone.forID(getCurrentUser().getCurrentSite().getTimeZone()));
						date = dtf.parseDateTime(config.getFilters().get(0).getValue()).toDate();
						break;
					case "met":
						metStationId = Integer.parseInt(filter.getValue());
						break;
					default:
						break;
					}
				}
			}
			if (date == null) {
				date = new Date();
			}
			page = getGridPageWeatherHistory(pageable, date, metStationId);
			dto = mapper.mapAsList(page.getContent(), MetAverageDTO.class);
			break;
		case MET_STATION:
			page = dataSourceManager.getMetStations(getCurrentSite(), pageable);
			dto =  mapper.mapAsList(page.getContent(), MetStationDTO.class);
			for(MetStationDTO met : (List<MetStationDTO> )dto) {
				if(MetStationType.EARTH_NETWORK_API.equals(met.getType())) {
					met.setUsername(null); //we do not want the clients to see our earth network key;
				}
			}
			break;
//		case SCENARIO_HISTORY:
//			page = scenarioManager.getScenarioRuns(getCurrentSite(), pageable);
//			for (Object o : page.getContent()) {
//				ScenarioRun sr = (ScenarioRun)o;
//				if (sr.getReleaseTime() != null) {
//					sr.setReleaseTime(DateUtils.convertToTimezone(sr.getReleaseTime(), getCurrentSite().getTimeZone()));
//				}
//			}
//			dto = mapper.mapAsList(page.getContent(), ScenarioHistoryDTO.class);
//			break;
		case SCENARIO:
		{
			Integer esId = null;
			if (config.getFilters() != null) {
				for (FilterConfig filter: config.getFilters()){
					switch (filter.getField()) {
					case Labels.FILTER_EMISSION_SOURCE_ID:
						esId = Integer.parseInt(filter.getValue());
						break;
					default:
						break;
					}
				}
			}
			List<ScenarioPredefinedGridDTO> scenarios = scenarioManager.getScenarioGridInfo(getCurrentSite(), esId);
			return (PagingLoadResult)new PagingLoadResultBean<ScenarioPredefinedGridDTO>(scenarios, scenarios.size(), 0);
		}
		case SENSOR:
		{
			page = dataSourceManager.getSensors(getCurrentSite(), pageable);
			dto = mapper.mapAsList(page.getContent(), SensorDTO.class);
			break;
		}
		case SENSOR_RAE:
		{
			page = dataSourceManager.getSensorsRae(getCurrentSite(), pageable);
			dto = mapper.mapAsList(page.getContent(), SensorRaeDTO.class);
			break;
		}
		case SENSOR_GROUP:
		{
			page = dataSourceManager.getSensorGroups(getCurrentSite(), pageable);
			dto = mapper.mapAsList(page.getContent(), SensorGroupDTO.class);
			break;
		}
		case SENSOR_INTERFACE:
		{
			List<SensorInterface> si = dataSourceManager.getSensorInterfaceBySite(getCurrentSite());
			return (PagingLoadResult)new PagingLoadResultBean<SensorInterfaceDTO>(mapper.mapAsList(si, SensorInterfaceDTO.class), si.size(), 0);
		}
		case SENSOR_INTERFACE_ENABLED:
		{
			List<SensorInterface> si = dataSourceManager.getSensorInterfaceEnabledBySiteAndTypeNotRae(getCurrentSite());
			return (PagingLoadResult)new PagingLoadResultBean<SensorInterfaceDTO>(mapper.mapAsList(si, SensorInterfaceDTO.class), si.size(), 0);
		}
		default:
			break;
		}
		PagingLoadResult result = new PagingLoadResultBean<UserDTO>(dto, (int) page.getTotalElements(), page.getNumber()*page.getSize());
		return result;
	}
	

	public Map<String, String> getConfigFilters(FilterPagingLoadConfig config){
		HashMap<String, String> filters = new HashMap<String, String>();
		if(config.getFilters() != null) {
			for (FilterConfig f:config.getFilters()){
				filters.put(f.getField(), f.getValue());
			}
		}
		return filters;
	}
	
	private Page<TipOfTheDay> getGridPageTipOfTheDay(FilterPagingLoadConfig config, PageRequest pageable) {
		Map<String, String> filters = getConfigFilters(config);
		return tipOfTheDayManager.getTipsOfTheDays( filters.get("enabled") != null 
				? Boolean.parseBoolean(filters.get("enabled")) : null, pageable);

	}
	
	private Page<Organization> getGridPageOrganizations(FilterPagingLoadConfig config, PageRequest pageable, String filterString) {
		Map<String, String> filters = getConfigFilters(config);
		if (filters.containsKey("selectAfterLoad")) {
			Integer selectAfterLoad = Integer.parseInt(filters.get("selectAfterLoad"));
			boolean hasSelectAfterLoad = false;
			Page<Organization> page = null;
			do {
				page = getGridPageOrganization(filters, pageable, filterString);
				for (Organization o: page.getContent()) {
					if (o.getId().equals(selectAfterLoad)){
						hasSelectAfterLoad = true;
						break;
					}
				}
				
				PageRequest next = new PageRequest(pageable.getPageNumber()+1, pageable.getPageSize(), pageable.getSort());
				pageable = next;
			} while(!hasSelectAfterLoad && page.getContent().size() > 0);
			return page;
		} else {
			return getGridPageOrganization(filters, pageable, filterString);
		}
	}

	private Page<Organization> getGridPageOrganization(Map<String, String> filters, PageRequest pageable, String filterString) {
		return organizationManager.getOrganizations( filters.get("enabled") != null ? Boolean.parseBoolean(filters.get("enabled")) : null, pageable, filterString);
	}
	
	private Page<MetAverage> getGridPageWeatherHistory(PageRequest pageable, Date date, Integer metStationId) {
		Date start, end;
		
		Site currentSite = getCurrentUser().getCurrentSite();
		String timeZone = currentSite.getTimeZone();
		MetStation metStation = dataSourceManager.getMetStation(metStationId);

		start = DateUtils.getUTCBeginnigOfTheDay(date, timeZone);
		end = DateUtils.getUTCEndOfTheDay(date, timeZone);
		
		List<MetAverage> averages = dataSourceManager.getMetAverages(metStation, MetAverageType.FIVE_MINUTE_AVERAGE, start, end);

		MetAverage yellowLineAverage = null;
		if (DateUtils.isToday(date, "Etc/UTC")) {
			//at a minute multiple of 5 we might not have the 5 min average in db. We need to get the partial average at the previous minute
			Date maxYellowAverageTime = null;
			
			if (averages.size() > 0) {
				Date latest5MinAveragesDate = averages.get(0).getDateTaken();
				Calendar curCal = DateUtils.trimToMinutes(Calendar.getInstance());
				if (!curCal.getTime().equals(latest5MinAveragesDate) && // we do not have the 5 min average at current date in db 
						curCal.get(Calendar.MINUTE)%5 == 0){ // current time is a multiple of five
					maxYellowAverageTime = new Date(curCal.getTime().getTime() - 30*1000);
				}
			}
			yellowLineAverage = getYellowLineAverage(metStation, maxYellowAverageTime);
		}
		if (yellowLineAverage != null) {
			averages.add(0, yellowLineAverage);
		}
		
		for (MetAverage avg: averages){
			if (avg.getAlarmLevelsData() == null) {
				avg.setAlarmLevelsData(new AlarmLevelsData());
			}
			avg.setDateTaken(DateUtils.convertToTimezone(avg.getDateTaken(), timeZone));
		}
		return new PageImpl<MetAverage>(averages, pageable, averages.size());
	}

	/**
	 * @param metStation
	 * @param maxAverageTime - when getting yellow line average for regular update (we have a selected date in history), 
	 * we use only the averages up to the selected history date
	 * @return
	 */
	public MetAverage getYellowLineAverage(MetStation metStation, Date maxAverageTime){
		List<MetAverage> yellowLineAverages = null;
		Calendar c = Calendar.getInstance();
		c.set(Calendar.MILLISECOND, 0);
		c.set(Calendar.SECOND, 0);
		Date end = maxAverageTime != null ? maxAverageTime : c.getTime();
		Date start = DateUtils.getLatestRelativeMinuteMultipleOfFive(end);
		start = new Date(start.getTime() + 30*1000);
		yellowLineAverages = dataSourceManager.getMetAverages(metStation, MetAverageType.ONE_MINUTE_AVERAGE, start, end);
		return getAverageOfAverage(yellowLineAverages);
	}
	@SuppressWarnings("deprecation")
	public static MetAverage getAverageOfAverage(List<MetAverage> averages){
		if (averages == null || averages.size() == 0) {
			return null;
		}
		MetData lastMetData = new MetData();
		List<double[]> winds = new ArrayList<double[]>();
		Date avgDate = null;
		Point location = null;
		int tempCount = 0;
		double tempSum = 0.0;
		int feelCount = 0;
		double feelSum = 0.0;
		double maxWindSpeed = -1.0;
		int humCount = 0;
		double humSum = 0.0;
		int solarCount = 0;
		double solarSum = 0.0;
		int pressureCount = 0;
		double pressureSum = 0.0;
		int hCount = 0;
		double hSum = 0;
		int vCount = 0;
		double vSum = 0;
		int halfAngleCount = 0;
		double halfAngleSum = 0;
		boolean isSolarCalculated = false;
		
		for (MetAverage avg: averages) {
			MetData data = avg.getMetData();
			if (data != null) {
				if (avgDate == null) {
					avgDate = avg.getDateTaken();
				}
				if (location == null) {
					location = avg.getLocation();
				}
				if (data.getWindDirection() != null && data.getWindSpeed() != null){
					winds.add(new double[]{data.getWindSpeed(), data.getWindDirection()});
					if (maxWindSpeed < data.getWindSpeed()) {
						maxWindSpeed = data.getWindSpeed();
					}
				}
				
				if (data.getTemperature() != null) {
					tempCount++;
					tempSum += data.getTemperature();
				}
				
				if (data.getFeelsLike() != null) {
					feelCount++;
					feelSum += data.getFeelsLike();
				}
				
				if (data.getHumidity() != null) {
					humCount++;
					humSum += data.getHumidity();
				}
				
				if (data.getSolarRadiation() != null) {
					solarCount++;
					solarSum += data.getSolarRadiation();
				}
				
				if (data.getPressure() != null) {
					pressureCount++;
					pressureSum += data.getPressure();
				}
				
				if (data.getCorridorHalfAngle() != null){
					halfAngleCount++;
					halfAngleSum += data.getCorridorHalfAngle();
				}
				
				if (Boolean.TRUE.equals(data.isSolarRadiationCalculated())){
					isSolarCalculated = true;
				}
				
				hCount++;
				hSum += data.gethStability();
				vCount++;
				vSum += data.getvStability();
			}
		}
		
		if (winds.size() > 0) {
			double[] sum = VectorUtils.sum(winds);
			lastMetData.setWindSpeed(sum[0]/winds.size());
			lastMetData.setWindDirection(sum[1]);
		}

		if (maxWindSpeed > 0) {
			lastMetData.setMaxWindSpeed(maxWindSpeed);
		}
		if (tempCount > 0) {
			lastMetData.setTemperature(tempSum/tempCount);
		}
		if (feelCount > 0) {
			lastMetData.setFeelsLike(feelSum/feelCount);
		}
		if (solarCount > 0) {
			lastMetData.setSolarRadiation(solarSum/solarCount);
		}
		if (pressureCount > 0) {
			lastMetData.setPressure(pressureSum/pressureCount);
		}
		if (humCount > 0) {
			lastMetData.setHumidity(humSum/humCount);
		}
		if (hCount > 0) {
			lastMetData.sethStability((int)Math.round(hSum/hCount));
		}
		if (vCount > 0) {
			lastMetData.setvStability((int)Math.round(vSum/vCount));
		}
		
		if (halfAngleCount > 0) {
			lastMetData.setCorridorHalfAngle(halfAngleSum/halfAngleCount);
		} else {
			lastMetData.setCorridorHalfAngle(0.0);
		}

		lastMetData.setSolarRadiationCalculated(isSolarCalculated);
		
		MetAverage lastAverage = new MetAverage();
		lastAverage.setDateTaken(avgDate);
		lastAverage.setMetStation(averages.get(averages.size()-1).getMetStation());
		lastAverage.setMetData(lastMetData);
		lastAverage.setAlarmLevelsData(averages.get(averages.size()-1).getAlarmLevelsData());
		lastAverage.setLocation(location);
		return lastAverage;
	}
	

	public Page<Site> getGridPageSite(PageRequest pageable, String filterValue) {
		Organization filter = null;
		if (!getCurrentUser().isSaferAdmin()) {
			filter = getCurrentUser().getOrganization();
		}
		if(UserUtils.isSiteAdminOnly(getCurrentUser().getRole())) {
			return siteManager.getSitesByUser(getCurrentUser(), pageable, filterValue);
		}else {
			return siteManager.getSites(filter, pageable, filterValue);	
		}
		
	}

	public Page<User> getGridPageUser(PageRequest pageable, Integer organizationId, String filterString) {
		if (!getCurrentUser().isSiteAdmin()) {
			return new PageImpl<User>(new ArrayList<User>());
		}
		Organization filter = null;
		if (!getCurrentUser().isSaferAdmin()) {
			filter = getCurrentUser().getOrganization();
		}
		if(organizationId!=null) {
			filter = new Organization();
			filter.setId(organizationId);
		}
		
		List<Site> sites = null;
		if (getCurrentUser().getRole() == Role.SITE_ADMIN) {
			sites = getCurrentUser().getSites();
		}
		List<Role> roles = new ArrayList<Role>();
		roles.addAll(Role.getAllRoles(getCurrentUser().getRole()));
		Page<User> usersPage = userManager.getUsers(filter, sites, roles, filterString, pageable);
		return usersPage;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends HasId> T getFormData(Integer id, RecordType formType, List<FilterConfig> extraInfo) {

		switch (formType) {
		case SITE:
			Site site = siteManager.getSite(id);
			return unitsManager.toCustom(mapOrNewInstance(site, SiteDTO.class), getCurrentSite().getId(),getCurrentUser().getId());
		case MET_STATION:
			MetStation met = dataSourceManager.getMetStation(id);
			EncryptionUtils.decryptCredentials(met);
			if(MetStationType.EARTH_NETWORK_API.equals(met.getType())) {
				met.setUsername(null); //we do not want the clients to see our earth network key;
			}
			MetStationDTO obj = unitsManager.toCustom(mapOrNewInstance(met, MetStationDTO.class) , getCurrentSite().getId(),getCurrentUser().getId());
			obj.setClickLocation(obj.getLocation());
			// TODO resolve Type safety: Unchecked cast from MetStationDTO to T
			return (T) obj;
		case SENSOR_INTERFACE:
			SensorInterface sen = dataSourceManager.getSensorInterfaceById(id);
			EncryptionUtils.decryptCredentials(sen);
			return (T) mapper.map(sen, SensorInterfaceDTO.class);
		case SENSOR:
			Sensor sensor = dataSourceManager.getSensor(id);
			SensorDTO internalUnitSensorDTO = mapper.map(sensor, SensorDTO.class);
			return (T) toCustomSensorDTO(internalUnitSensorDTO);
		case SENSOR_RAE:
			return (T) mapper.map(dataSourceManager.getSensorRaeById(id), SensorRaeDTO.class);
		case SENSOR_GROUP:
			SensorGroup group = dataSourceManager.getSensorGroupById(id);
			SensorGroupDTO sensorGroupDTO = unitsManager.toCustom(mapper.map(group, SensorGroupDTO.class), getCurrentSite().getId(),getCurrentUser().getId());
			List<Sensor> sensors = dataSourceManager.getSensorsByGroup(group);
			sensorGroupDTO.setSensors(mapper.mapAsList(sensors, SensorDTO.class));
			for(SensorDTO customUnitSensor : sensorGroupDTO.getSensors()) {
				toCustomSensorDTO(customUnitSensor);
			}
			return (T) sensorGroupDTO; 
		case POI:
			PointOfInterest poi = poisManager.getPoiById(id);
			return mapOrNewInstance(poi, PointOfInterestDTO.class);
		case EMISSION_SOURCE:
			EmissionSource em = emissionManager.getEmissionSource(id);
			return (T) unitsManager.toCustom(getEmissionSourceDTO(em), getCurrentSite().getId(),getCurrentUser().getId());
		case SCENARIO_RUN:
		case IGNITE_ME:			
		{
			ScenarioRun sr = scenarioManager.getScenarioRun(id);
			
			if (formType == RecordType.IGNITE_ME) {
				sr.setScenarioType(getPostDispersionFireType(sr.getScenarioType()));
				sr.setId(null);
				sr.getScenario().setSurfaceRadiation(Config.DEFAULT_SOURCE_RADIATION);
			}
			
			if (sr.getReleaseTime() != null) {
				sr.setReleaseTime(DateUtils.convertToTimezone(sr.getReleaseTime(), getCurrentSite().getTimeZone()));
			}
			EmissionSourceDTO emDTO = getScenarioRunDTO(sr);
			if(emDTO instanceof ScenarioRunDTO) {
				if(((ScenarioRunDTO)emDTO).getManualMetData() != null) {
					for(ScenarioMetDataDTO metData : ((ScenarioRunDTO)emDTO).getManualMetData()) {
						unitsManager.toCustom(metData.getMetData(), getCurrentSite().getId(),getCurrentUser().getId());	
					}
				}
			}
			

			return (T) unitsManager.toCustom(emDTO, getCurrentSite().getId(),getCurrentUser().getId());
		}
		case USER:
			User user = userManager.getUser(id);
			return mapOrNewInstance(user, EditUserDTO.class);
		case ORGANIZATION:
			if (getCurrentUser().isSaferAdmin() || getCurrentUser().isOrganizationAdmin()
					&& getCurrentUser().getOrganization().getId().equals( id)) {
				Organization org = organizationManager.getOrganization(id);
				return mapOrNewInstance(org, OrganizationDTO.class);
			}
			throw new RuntimeException("You are not allowed to edit organizations");
		case EVENT:
			return (T) getErgEvent(id);
		case DAQ:
			Daq daq = daqManager.getDaq(id);
			if (Boolean.TRUE.equals(daq.getDefaultDaq()) && !UserUtils.isSaferAdmin(getCurrentUser().getRole())) {
				throw new RuntimeException(Labels.CANT_EDIT_THIS_DAQ);
			}
			return mapOrNewInstance(daq, DaqDTO.class);
		case PORT:
			Port port = daqManager.getPort(id);
			return mapOrNewInstance(port, PortDTO.class);
		case SCENARIO_RUN_FROM_EMISSION_SOURCE:
		{
			EmissionSource es = emissionManager.getEmissionSource(id);

			//site and location breaks the mapping from EmissionSource to ScenarioRun
			//we do not need site for scenarioRun
			es.setSite(null);
			Integer esId = es.getId(); 
			es.setId(null);
			
			if (extraInfo == null) {
				String message = "Missing extra info when starting scenario from emissions source " + id;
				logger.error(message);
				throw new RuntimeException(message);
			}
			ScenarioType scenarioType = null;
			for (FilterConfig f : extraInfo) {
				if (f.getField().equals("scenarioType")) {
					scenarioType = ScenarioType.valueOf(f.getValue());
				}
			}
			if (scenarioType == null) {
				String message = "Missing scenario type when starting scenario from emissions source " + id;
				logger.error(message);
				throw new RuntimeException(message);
			}
			
			ScenarioRun scenarioRun = new ScenarioRun();
			Scenario scenario = new Scenario();
			scenario.setScenarioType(scenarioType);
			scenario.setEmissionSource(es);
			scenario.initNonESFields();
			
			scenarioRun.setScenario(scenario);
			scenarioRun.setScenarioType(scenarioType);
			EmissionSourceDTO scenarioRunDTO = getScenarioRunDTO(scenarioRun);
			if (scenarioRunDTO instanceof ScenarioRunDTO) {
				((ScenarioRunDTO)scenarioRunDTO).setEmissionSourceId(esId);
				((ScenarioRunDTO)scenarioRunDTO).setEmissionSourceName(scenario.getEmissionSource().getName());
			}
			return (T) unitsManager.toCustom(scenarioRunDTO, getCurrentSite().getId(),getCurrentUser().getId());
		}
		case SCENARIO_RUN_FROM_PREDEFINED:
		case SCENARIO:
		{
			Scenario scenario = scenarioManager.getScenario(id);
			ScenarioRun sr = new ScenarioRun();
			sr.setScenario(scenario);
			sr.setScenarioType(scenario.getScenarioType());
			sr.setManualNameChange(false);
			EmissionSourceDTO dto = getScenarioRunDTO(sr);
			dto.setName(scenario.getName());
			if (formType == RecordType.SCENARIO) {
				dto.setId(id);
			}
			((ScenarioRunDTO)dto).setEmissionSourceName(scenario.getEmissionSource().getName());
			return (T) unitsManager.toCustom(dto, getCurrentSite().getId(),getCurrentUser().getId());
		}
		case FEEDBACK:
			Feedback feedback = feedbackManager.getFeedback(id);
			FeedbackDTO feedbackDTO = mapper.map(feedback, FeedbackDTO.class);
			feedbackDTO.setScreenshot(feedback.getImage()!=null);
			feedbackDTO.setCreatedDate(DateUtils.convertToTimezone(feedbackDTO.getCreatedDate(), getCurrentSite().getTimeZone()));
			return (T) feedbackDTO;
		case TIPOFTHEDAY:
			if (getCurrentUser().isSaferAdmin()) {
				TipOfTheDay org = tipOfTheDayManager.getTipOfTheDay(id);
				return mapOrNewInstance(org, TipOfTheDayDTO.class);
			}
			throw new RuntimeException("You are not allowed to edit organizations");
		default:
			throw new RuntimeException("Please update getFormData to also include " + formType.toString());
		}
	}

	private SensorDTO toCustomSensorDTO(SensorDTO internalUnitSensorDTO) {
		SensorDTO sensorDTO = unitsManager.toCustom(internalUnitSensorDTO, getCurrentSite().getId(), getCurrentUser().getId());
		SensorAlarmValuesDTO alarms = sensorDTO.getAlarmValues();
		if(!SensorType.isCustomUnitType(sensorDTO.getType()) && alarms!=null) {
			alarms.setAlarmLev1(getUnits().toCustomAllowNull(UnitParam.IDP_SENSOR_READING, alarms.getAlarmLev1()));
			alarms.setAlarmLev2(getUnits().toCustomAllowNull(UnitParam.IDP_SENSOR_READING, alarms.getAlarmLev2()));
			alarms.setMinLevel(getUnits().toCustomAllowNull(UnitParam.IDP_SENSOR_READING, alarms.getMinLevel()));
			alarms.setDeadband(getUnits().toCustomAllowNull(UnitParam.IDP_SENSOR_READING, alarms.getDeadband()));
			alarms.setSatuLev(getUnits().toCustomAllowNull(UnitParam.IDP_SENSOR_READING, alarms.getSatuLev()));
		}
		return sensorDTO;
	}
	
	private ScenarioType getPostDispersionFireType(ScenarioType scenarioType) {
		if (scenarioType.isDispersion()) {
			switch (scenarioType) {
			case GAS_RELEASE:
				return ScenarioType.POST_DISPERSION_GAS_FIRE;
			case LIQUID_RELEASE:
				return ScenarioType.POST_DISPERSION_LIQUID_FIRE;
			case PIPE_RELEASE:
				return ScenarioType.POST_DISPERSION_PIPE_FIRE;
			case TANK_RELEASE:
				return ScenarioType.POST_DISPERSION_TANK_FIRE;
			default:
				break;
			}
		}
		return null;
	}

	private EmissionSourceDTO getScenarioRunDTO(ScenarioRun sr) {
		EmissionSourceDTO srDTO;
		EmissionSource emissionSource = sr.getScenario().getEmissionSource();
		switch (sr.getScenarioType()) {
		case GAS_RELEASE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunGasReleaseDTO.class);
			break;
		case TANK_RELEASE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunTankReleaseDTO.class);
			break;
		case PIPE_RELEASE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunPipeReleaseDTO.class);
			break;
		case STACK_RELEASE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunStackReleaseDTO.class);
			break;
		case PUDDLE_RELEASE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunPuddleReleaseDTO.class);
			break;
		case PARTICULATE_RELEASE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunParticulateReleaseDTO.class);
			break;
		case LIQUID_RELEASE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunLiquidReleaseDTO.class);
			break;
		case SAL:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunSensorInputDTO.class);
			break;
		case ABC:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunSensorInputDTO.class);
			break;
		case POOL_FIRE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunPoolFireDTO.class);
			break;
		case JET_FIRE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunJetFireDTO.class);
			break;
		case TANK_TOP_FIRE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunTankTopFireDTO.class);
			break;
		case POST_DISPERSION_GAS_FIRE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunPostDispersionGasFireDTO.class);
			break;
		case POST_DISPERSION_LIQUID_FIRE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunPostDispersionLiquidFireDTO.class);
			break;
		case POST_DISPERSION_PIPE_FIRE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunPostDispersionPipeFireDTO.class);
			break;
		case POST_DISPERSION_TANK_FIRE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunPostDispersionTankFireDTO.class);
			break;
		case FIREBALL:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunFireballDTO.class);
			break;
		case VESSEL_BURST:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunVesselBurstDTO.class);
			break;
		case VAPOR_CLOUD_EXPLOSION:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunVaporCloudDTO.class);
			break;
		case SOLID_EXPLOSIVE:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunSolidExplosiveDTO.class);
			break;
		case POST_DISPERSION_GAS_EXPLOSION:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunPostDispersionGasExplosionDTO.class);
			break;
		case POST_DISPERSION_LIQUID_EXPLOSION:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunPostDispersionLiquidExplosionDTO.class);
			break;
		case POST_DISPERSION_PIPE_EXPLOSION:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunPostDispersionPipeExplosionDTO.class);
			break;
		case POST_DISPERSION_TANK_EXPLOSION:
			srDTO = mapOrNewInstance(emissionSource, ScenarioRunPostDispersionTankExplosionDTO.class);
			break;
		default:
			return mapOrNewInstance(emissionSource, EmissionSourceDTO.class);
		}
		mapper.map(sr.getScenario(), srDTO);
		mapper.map(sr, srDTO);
		if (srDTO instanceof ScenarioRunDTO) {
			((ScenarioRunDTO)srDTO).setEmissionSourceId(emissionSource.getId());
		}
		
		if (emissionSource.getType() == EmissionSourceType.TANK && sr.getScenario().getScenarioType() == ScenarioType.VESSEL_BURST) {
			if (sr.getScenario().getVesselShape() == null) {
				switch (emissionSource.getTankType()) {
				case HORIZONTAL_CYLINDER:
					sr.getScenario().setVesselShape(VesselShape.CYLINDER);
					sr.getScenario().setVesselVolume(emissionSource.getTankDiameter()/2*emissionSource.getTankDiameter()/2*Math.PI*emissionSource.getTankLength());
					break;
				case VERTICAL_CYLINDER:
					sr.getScenario().setVesselShape(VesselShape.CYLINDER);
					sr.getScenario().setVesselVolume(emissionSource.getTankDiameter()/2*emissionSource.getTankDiameter()/2*Math.PI*emissionSource.getTankHeight());
					break;
				case SPHERE:
					sr.getScenario().setVesselShape(VesselShape.SPHERE);
					sr.getScenario().setVesselVolume(4.0/3.0*Math.pow(emissionSource.getTankDiameter()/2, 3)*Math.PI);
					break;
				case RECTANGLE:
					sr.getScenario().setVesselVolume(emissionSource.getTankLength()*emissionSource.getTankHeight()*emissionSource.getTankWidth());
					break;
				}
			}
			
			
		}
		return srDTO;
	}
	
	private EmissionSourceDTO getEmissionSourceDTO(EmissionSource em) {
		switch (em.getType()) {
		case GAS:
			return mapOrNewInstance(em, EmissionSourceGasDTO.class);
		case LIQUID:
			return mapOrNewInstance(em, EmissionSourceLiquidDTO.class);
		case PARTICULATE:
			return mapOrNewInstance(em, EmissionSourceParticulateDTO.class);
		case PIPE:
			return mapOrNewInstance(em, EmissionSourcePipeDTO.class);
		case POOL:
			return mapOrNewInstance(em, EmissionSourcePoolDTO.class);
		case STACK:
			return mapOrNewInstance(em, EmissionSourceStackDTO.class);
		case PUDDLE:
			return mapOrNewInstance(em, EmissionSourcePuddleDTO.class);
		case TANK:
			return mapOrNewInstance(em, EmissionSourceTankDTO.class);			
		default:
			return mapOrNewInstance(em, EmissionSourceDTO.class);
		}
	}

	public ErgEventDTO getErgEvent(Integer id) {
		ErgEvent event = eventManager.getErgEvent(id);
		if (event.getMetStation() != null && event.getMetStation().getHidden() == true) {
			event.setMetStation(null);
		}

		updateErgMetStation(event);
		if (event.getReleaseTime() != null) {
			event.setReleaseTime(DateUtils.convertToTimezone(event.getReleaseTime(), getCurrentUser().getCurrentSite().getTimeZone()));
		}
		if (event.getLatestMetAverageTime() != null) {
			event.setLatestMetAverageTime(DateUtils.convertToTimezone(event.getLatestMetAverageTime(), getCurrentUser().getCurrentSite().getTimeZone()));
		}
		return mapOrNewInstance(event, ErgEventDTO.class);
	}

	public void updateErgMetStation(ErgEvent event) {
		if (event.getMetDataType() == null){
			event.setMetDataType(ErgMetDataType.MANUAL_INPUT);
		}
		switch (event.getMetDataType()) {
		case MANUAL_INPUT:
		case MANUAL_FORM:
			MetStation met = new MetStation();
			met.setId(-1);
			met.setName("Manual Input");
			event.setMetStation(met);
			break;
		default:
			break;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	@Secured("ROLE_SITE_ADMIN")
	public <T extends HasId> T moveFormData(int id, RecordType formType, PointDTO position, boolean enableAtPaste, Map<String,Object> extra) throws FieldValidationException {
		try {
			switch (formType) {
			case POI:
				return (T) movePoi(id, position);
			case EMISSION_SOURCE:
				return (T) moveEmissionSource(id, position);
			case MET_STATION:
				return (T) moveMetStation(id, position, enableAtPaste);
			case SENSOR:
				return (T) moveSensor(id, position, enableAtPaste, extra);
			case SENSOR_RAE:
				return (T) moveSensorRae(id, position, enableAtPaste);
			case SENSOR_GROUP:
				return (T) moveSensorGroup(id, position, enableAtPaste, extra);
			default:
				throw new RuntimeException("Please update moveFormData for type:" + formType);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	@Secured("ROLE_SITE_ADMIN")
	protected <T extends HasId> T movePoi(int id, PointDTO position) throws FieldValidationException {
		PointOfInterest poi = poisManager.getPoiById(id);
		if (poi == null) {
			throw new FieldValidationException("Invalid point of interest");
		}
		poi.setLocation(mapper.map(position, Point.class));
		poi = poisManager.save(poi);
		return mapOrNewInstance(poi, PointOfInterestDTO.class);
	}
	
	@Secured("ROLE_SITE_ADMIN")
	protected EmissionSourceDTO moveEmissionSource(int id, PointDTO position) throws FieldValidationException {
		EmissionSource em = emissionManager.getEmissionSource(id);
		if (em == null) {
			throw new FieldValidationException("Invalid emission source");
		}
		em.setLocation(mapper.map(position, Point.class));
		em = emissionManager.save(em);

		return mapper.map(em, EmissionSourceDTO.class);
	}
	
	@Secured("ROLE_SITE_ADMIN")
	protected <T extends HasId> T moveMetStation(int id, PointDTO position, boolean enableAtPaste) throws FieldValidationException {
		MetStation met = dataSourceManager.getMetStation(id);
		if (met == null) {
			throw new FieldValidationException("Invalid met station");
		}
		met.setLocation(mapper.map(position, Point.class));
		met.setLastLocationChangeDate(new Date());
		if (enableAtPaste) {
			met.setEnabled(enableAtPaste);
		}
		met = dataSourceManager.save(met);

		return unitsManager.toCustom(mapOrNewInstance(met, MetStationDTO.class), getCurrentSite().getId(),getCurrentUser().getId());
	}
	
	
	protected <T extends HasId> T moveSensor(int id, PointDTO position, boolean enableAtPaste, Map<String,Object> extra) throws FieldValidationException {
		Sensor sensor = dataSourceManager.getSensor(id);
		if (sensor == null) {
			throw new FieldValidationException("Invalid sensor");
		}
		boolean isReflector = extra!=null && extra.containsKey(Config.REFLECTOR_KEY);
		if(isReflector) {
			sensor.setReflectorLocation(mapper.map(position, Point.class));
		}else { 
			sensor.setLocation(mapper.map(position, Point.class));
			sensor.setLastLocationChangeDate(new Date());
		}
		sensor.setModifiedDate(new Date());
		if (enableAtPaste) {
			sensor.setEnabled(enableAtPaste);
		}
		sensor = dataSourceManager.saveSensor(sensor);

		return unitsManager.toCustom(mapOrNewInstance(sensor, SensorDTO.class), getCurrentSite().getId(),getCurrentUser().getId());
	}
	
	protected <T extends HasId> T moveSensorRae(int id, PointDTO position, boolean enableAtPaste) throws FieldValidationException {
		SensorRae rae = dataSourceManager.getSensorRaeById(id);
		if (rae == null) {
			throw new FieldValidationException("Invalid sensor rae");
		}
		Date currentDate = new Date();
		rae.setLocation(mapper.map(position, Point.class));
		rae.setLastLocationChangeDate(currentDate);
		rae.setModifiedDate(new Date());
		rae.setGpsEnabled(false);
		for(Sensor sensor : dataSourceManager.getSensorsByRae(rae)) {
			sensor.setLocation(mapper.map(position, Point.class));
			sensor.setLastLocationChangeDate(currentDate);
			dataSourceManager.saveSensor(sensor);
		}
		if (enableAtPaste) {
			rae.setEnabled(enableAtPaste);
		}
		rae = dataSourceManager.saveSensorRae(rae);

		return unitsManager.toCustom(mapOrNewInstance(rae, SensorRaeDTO.class), getCurrentSite().getId(),getCurrentUser().getId());
	}
	
	protected <T extends HasId> T moveSensorGroup(int id, PointDTO position, boolean enableAtPaste, Map<String,Object> extra) throws FieldValidationException {
		SensorGroup group = dataSourceManager.getSensorGroupById(id);
		if (group == null) {
			throw new FieldValidationException("Invalid sensor");
		}
		Date currentDate = new Date();
		boolean isReflector = extra!=null && extra.containsKey(Config.REFLECTOR_KEY);
		if(isReflector) {
			group.setReflectorLocation(mapper.map(position, Point.class));
		}else {
			group.setLocation(mapper.map(position, Point.class));
		}
		group.setLastLocationChangeDate(currentDate);
		group.setModifiedDate(new Date());
		for(Sensor sensor : dataSourceManager.getSensorsByGroup(group)) {
			if(isReflector) {
				sensor.setReflectorLocation(mapper.map(position, Point.class));
			}else {
				sensor.setLocation(mapper.map(position, Point.class));
			}
			sensor.setLastLocationChangeDate(currentDate);
			dataSourceManager.saveSensor(sensor);
		}
		if (enableAtPaste) {
			group.setEnabled(enableAtPaste);
		}
		group = dataSourceManager.saveSensorGroup(group);

		return unitsManager.toCustom(mapOrNewInstance(group, SensorGroupDTO.class), getCurrentSite().getId(),getCurrentUser().getId());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	@Secured("ROLE_SITE_ADMIN")
	public <T extends HasId> T cloneFormData(int id, RecordType formType, PointDTO position, Integer groupId, Boolean pasteWithScenarios) throws FieldValidationException {
		try {
			switch (formType) {
			case POI:
				return (T) clonePoi(id, position, groupId);
			case EMISSION_SOURCE:
				return (T) cloneEmissionSource(id, position, pasteWithScenarios);
			case EVENT:
				return (T) cloneErgEvent(id);
			case SENSOR:
				return (T) cloneSensor(id, position);
			default:
				throw new RuntimeException("Please update cloneFormData for type:" + formType);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	protected ErgEventDTO cloneErgEvent(int id) throws FieldValidationException {
		ErgEvent event = eventManager.getErgEvent(id);
		
		if (!getCurrentUser().getSites().contains(event.getSite())) {
			throw new FieldValidationException("You can clone only events from the sites you can access");
		}
		event.setId(null);
		event.setUser(getCurrentUser());
		event.setCreatedDate(new Date());

		if (!event.getName().startsWith("Clone of")) {
			event.setName("Clone of " + event.getName());
		}
		event = eventManager.saveErgEvent(event);
		event.setReleaseTime(DateUtils.convertToTimezone(event.getReleaseTime(), getCurrentSite().getTimeZone()));
		
		cloneEventPlaces(id, event.getGroupId());
		cloneEventZones(id, event.getGroupId());
		
		return mapOrNewInstance(event, ErgEventDTO.class);
	}
	
	protected void cloneEventZones(int oldEventId, int newEventGroupId) {
		 cloneEventZones(oldEventId, newEventGroupId, getCurrentSite());
	}
	
	protected void cloneEventPlaces(int oldGroupId, int newGroupId) {
		 cloneEventPlaces(oldGroupId, newGroupId, getCurrentSite());
	}
	
	protected void cloneEventZones(int oldEventId, int newEventGroupId, Site site) {
		List<Zone> eventZones =  zoneManager.getZones(site,oldEventId);
		for(Zone zone : eventZones) {
			zone.setId(null);
			zone.setGroupId(newEventGroupId);
			zoneManager.save(zone);
		}
	}
	
	protected void cloneEventPlaces(int oldGroupId, int newGroupId, Site site) {
		List<PointOfInterest> pois = poisManager.getPoiRepository().findBySiteAndHiddenFalseAndGroupId(site,oldGroupId);
		for(PointOfInterest poi : pois) {
			poi.setId(null);
			poi.setGroupId(newGroupId);
			poisManager.save(poi);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends HasId> T saveFormData(T data, RecordType formType) throws FieldValidationException {
		data = (T) HtmlUtils.escapeHtml(data);
		try {
			switch (formType) {
			case SITE:
				return (T) saveSite((SiteDTO) data);
			case MET_STATION:
				return saveMetStation((MetStationDTO) data);
			case SENSOR_INTERFACE:
				return saveSensorInterface((SensorInterfaceDTO)data);
			case SENSOR_GROUP:
				return saveSensorGroup((SensorGroupDTO)data);
			case POI:
				return (T) savePoi((PointOfInterestDTO) data);
			case USER:
				return (T) saveUser((EditUserDTO) data);
			case ORGANIZATION:
				return (T) saveOrganization((OrganizationDTO) data);
			case EMISSION_SOURCE:
				return (T) saveEmissionSource((EmissionSourceDTO) data);
			case EVENT:
				return (T) saveErgEvent((ErgEventDTO) data);
			case DAQ:
				return (T) saveDaq((DaqDTO) data);
			case PORT:
				return (T) savePort((PortDTO) data);
			case MANUAL_MET:
				return (T) saveManualMet((ManualMetDataDTO) data);
			case SITE_SETTINGS:
				return (T) saveSiteSettings((SiteSettingsDTO) data);
			case SCENARIO_RUN:
				return (T) runScenario((ScenarioRunDTO) data, getCurrentUser(), getCurrentSite());
			case SCENARIO:
				return (T) saveScenario((EmissionSourceDTO) data);				
			case CHEMICAL:
				return (T) saveChemical((ChemicalDetailsDTO) data);
			case SENSOR:
				return (T) saveSensor((SensorDTO) data);
			case SENSOR_RAE:
				return (T) saveSensorRae((SensorRaeDTO) data);
			case FEEDBACK:
				return (T) saveFeedback((FeedbackDTO) data); 
			case TIPOFTHEDAY:
				return (T) saveTipOfTheDay((TipOfTheDayDTO) data); 
			case ORGANIZATION_SETTING:
				return (T) saveOrganizationSettings((OrganizationDTO) data); 
			default:
				throw new RuntimeException("Please update saveFormData for type:" + formType);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	private ChemicalDetailsDTO saveChemical(ChemicalDetailsDTO data) {
		Integer siteId = getCurrentSite().getId();
		
		unitsManager.toInternal(data, siteId,getCurrentUser().getId());
		
		for(ConcentrationIsoplethsDTO concIso : data.getConcentrationIsoplethsList()) {
			unitsManager.toInternal( concIso, siteId,getCurrentUser().getId());
		}
		if(data.getParticulateIsopleths()!=null) {
			unitsManager.toInternal( data.getParticulateIsopleths(), siteId,getCurrentUser().getId());

		}
		
		Chemical chem = chemicalsManager.saveChemicalDetails(data, getCurrentSite());
		return getChemical(chem.getId(),false, null);
	}

	public boolean importChemical(String dataFile, String logFile, List<Integer> selectedIds, Site site) {
		
		PrintWriter pw = null;
		
		try {
			
			pw = new PrintWriter(logFile);
				
			ObjectMapper omap = new ObjectMapper();
			
			ChemicalDetailsDTO[] data = new ChemicalDetailsDTO[0];
			
			pw.println("Reading data file...");
			
			try {
				data = omap.readValue(new File(dataFile), ChemicalDetailsDTO[].class);
			} catch (IOException e) {
				pw.printf("ERROR: Failed to read/access data file. (%s)\r\n", e.getMessage());
				return false;
			}				
			pw.printf("Data file successfully read, total records %d, selected records %d\r\n", data.length, selectedIds.size());
			
			
			boolean addRec = false;
			int rtId = 0;
			ChemicalDetailsDTO record = null;
			Chemical chem = null;
			String notes = "";
			
			for (int i = 0; i < data.length; i++) {
				
				record = data[i];
				rtId = record.getId();
				if (!selectedIds.contains(rtId)) //skip if not selected
					continue;
								
				addRec = true;
				
				if (!record.getNotes().equals("")) {
					notes = "<div>" + record.getNotes().replaceAll("\\r\\n\\r\\n", "</div><br><div>");
					notes = notes.replaceAll("\\r\\n", "</div><div>");
					notes += "</div>";
					record.setNotes(notes);					
				}								
				
				//set as a new chemical
				record.setId(null);
				record.setEnabled(true);			
				
				//initialize Antoine, Wilson if null
				if (record.getAntoineMixtureDetails() == null)
					record.setAntoineMixtureDetails(new ArrayList<AntoineMixtureDetailsDTO>());
				if (record.getWilsonMixtureDetails() == null)
					record.setWilsonMixtureDetails(new ArrayList<WilsonMixtureDetailsDTO>());			
			
				//for mixture use component from existing records
				if (record.getCategory() == ChemicalType.MIXTURE) {										
					ChemicalDetailsDTO cnv = calculateMixtureChemicalImport(record.getMixtureChemicals(), record.getMixtureModelType(), record.getMixtureCompositionAmount());
					
					cnv.setId(null); //set as a new chemical
					cnv.setEnabled(true);				
					cnv.setName(record.getName());
					cnv.setMixtureChemicalIds("");
					cnv.setNotes(record.getNotes());
					cnv.setHazmatInfo("");
					cnv.setMixtureModelType(record.getMixtureModelType());
					cnv.setAntoineMixtureDetails(record.getAntoineMixtureDetails());
					cnv.setWilsonMixtureDetails(record.getWilsonMixtureDetails());
					cnv.setMixtureCompositionAmount(record.getMixtureCompositionAmount());
										
					chem = chemicalsManager.saveChemicalDetails(cnv, site);
					//write to log file the chemical is added
					addRec = false;
					if (cnv.getMixtureChemicalsKeyComponentIndex() < 0)
						pw.printf("Added (create isopleth): %s [RT=%d, S1=%d]\r\n", record.getName(), rtId, chem.getId());
					else
						pw.printf("Added: %s [RT=%d, S1=%d]\r\n", record.getName(), rtId, chem.getId());
					
				}
				else if (record.getSaferNo() > 0) {
					
					if (record.getSaferNo() == 9500)
						record.setSaferNo(9501); //use new number
					else if (record.getSaferNo() == 9600)
						record.setSaferNo(9601); //use new number
					
					//check if safer no. is present
					chem = chemicalsManager.findBySaferNo(record.getSaferNo());
					
					if (record.getSaferNo() == 9300) {
						//Obsolete predefined solution, skip
						addRec = false;
						pw.printf("Obsolete: %s [RT=%d]\r\n", record.getName(), rtId);						
					}						
					else if (record.getSaferNo() == 9000 || record.getSaferNo() == 9100 ||
							 record.getSaferNo() == 9200 || record.getSaferNo() == 9400 ||
							 record.getSaferNo() == 9700 || record.getSaferNo() == 9800 ) {
						//Predefined solution, skip
						addRec = false;
						pw.printf("Skipped (Solution, add manually): %s [RT=%d]\r\n", record.getName(), rtId);						
					}						
					else if (chem == null) {
						addRec = false;
						pw.printf("Missing: %s [RT=%d]\r\n", record.getName(), rtId);						
					}
					else if (chem.getSaferNo().equals(record.getSaferNo())) {				
						// skip, write to log as Chemical present
						addRec = false;
						pw.printf("Exist: %s -> %s [RT=%d, S1=%d]\r\n", record.getName(), chem.getName(), rtId, chem.getId());						
					}
				}
							
				if (addRec) {										
					chem = chemicalsManager.saveChemicalDetails(record, site);
					// write to log file the chemical is added
					pw.printf("Added (create isopleth): %s -> %s [RT=%d, S1=%d]\r\n", record.getName(), chem.getName(), rtId, chem.getId());
				}
			}
			
			pw.println("Import completed.");
		
		} catch (FileNotFoundException e1) {
			return false;
		}
		finally {
			if (pw != null)
				pw.close();
		}
		
		return true;
	}	
	
	
	@Transactional
	private ErgEventDTO saveErgEvent(ErgEventDTO data) throws FieldValidationException {
		
		if (!validator.validate(data).isEmpty()) {
			throw new FieldValidationException("Invalid data");
		}
		
		SiteSettings siteSettings = siteManager.getSiteSettings(getCurrentSite());
		if(!siteSettings.isErg()){
			throw new RuntimeException("Your license doesn't alow running ERG events");
		}
		
		if (data.getReleaseTime() != null) {
			data.setReleaseTime(DateUtils.trimToMinutes(data.getReleaseTime()));
		}
		if (data.getMetDataType() != ErgMetDataType.MET) {
			data.setMetStation(null);
		}
		
		ErgEvent ergEvent = null;
		if (data.getId() == null) {
			ergEvent = mapper.map(data, ErgEvent.class);
			ergEvent.setCreatedDate(new Date());
			ergEvent.setSite(getCurrentUser().getCurrentSite());
			ergEvent.setUser(getCurrentUser());
		} else {
			if (data.getUser() == null || !mapper.map(data.getUser(), User.class).equals(getCurrentUser())) {
				throw new FieldValidationException("Users can edit only events they own");
			}
			ergEvent = eventManager.getErgEvent(data.getId());

			if (ergEvent.getUser() == null || !ergEvent.getUser().equals(getCurrentUser())) {
				throw new FieldValidationException("You can't steal an event!");
			}
			mapper.map(data, ergEvent);
		}
		
		if (ergEvent.getReleaseTime() != null){
			ergEvent.setReleaseTime(DateUtils.convertFromTimezoneToUtc(ergEvent.getReleaseTime(), getCurrentUser().getCurrentSite().getTimeZone()));
		}
		if (ergEvent.getLatestMetAverageTime() != null) {
			ergEvent.setLatestMetAverageTime(DateUtils.convertFromTimezoneToUtc(ergEvent.getLatestMetAverageTime(), getCurrentUser().getCurrentSite().getTimeZone()));
		}
		ergEvent = eventManager.saveErgEvent(ergEvent);
		ergEvent.setReleaseTime(data.getReleaseTime());
		ergEvent.setLatestMetAverageTime(data.getLatestMetAverageTime());
		
		updateErgMetStation(ergEvent);
		
		
		ErgEventDTO ret = mapOrNewInstance(ergEvent, ErgEventDTO.class);
		//TODO Cristi - this line can be removed if we rename  GoupId with groupId
		ret.setGroupId(ergEvent.getGroupId());
		
		return ret;
	}

	@Secured("ROLE_SITE_ADMIN")
	protected <T extends HasId> T saveMetStation(MetStationDTO data) throws FieldValidationException {
		if (!getCurrentUser().isSiteAdmin()) {
			throw new FieldValidationException("You don't have the rights to edit met stations");
		}

		unitsManager.toInternal(data, getCurrentSite().getId(),getCurrentUser().getId());
		if (EarthCalc.getDistance(data.getLocation().asGeoCalcPoint(), new PointDTO(getCurrentSite().getLatitude(), getCurrentSite()
				.getLongitude()).asGeoCalcPoint()) > getCurrentSite().getRadius()) {
			throw new FieldValidationException("Location outside site range");
		}
		MetStation met;

		boolean isAddNew = (data.getId() == null);
		boolean isEarthNetwork = data.getType() == MetStationType.EARTH_NETWORK_API;
		boolean isEarthNetworkStationChanged = false;

		
		if (isAddNew) {
			if (Boolean.TRUE.equals(data.isEnabled()) && Config.MAX_ACTIVE_MET_PER_SITE <= dataSourceManager.countEnabledMetStationsPerSite(getCurrentSite().getId())){
				throw new FieldValidationException(Labels.MAX_ACTIVE_METSTATIONS);
			}
			met = mapper.map(data, MetStation.class);
		} else {
			met = dataSourceManager.getMetStation(data.getId());
			if (Boolean.TRUE.equals(data.isEnabled()) && Boolean.FALSE.equals(met.getEnabled()) && Config.MAX_ACTIVE_MET_PER_SITE <= dataSourceManager.countEnabledMetStationsPerSite(getCurrentSite().getId())){
				throw new FieldValidationException(Labels.MAX_ACTIVE_METSTATIONS);
			}
			PointDTO oldPosition = new PointDTO(met.getLocation().getX(), met.getLocation().getY());
			PointDTO newPosition = data.getLocation();
			if (isEarthNetwork) {
				isEarthNetworkStationChanged = !met.getEarthNetworkInfo().getApiStation().getApiStationId()
						.equals(data.getEarthNetworkInfo().getApiStation().getApiStationId());
			}
			mapper.map(data, met);
			if (!oldPosition.equals(newPosition)) {
				met.setLastLocationChangeDate(new Date());
			}
			// If we don't do this, the Daq fields will get overwritten by the
			// fields belonging to the Daq object on the ports, because the ORM
			// uses the same reference for both
			mapper.map(data.getDaq(), met.getDaq());
		}
		
		if(isEarthNetwork) {
			met.setUsername(getEarthNetworkKey());
		}
		EncryptionUtils.encryptCredentials(met);
		
		met = dataSourceManager.save(met);
		if (met.getEnabled() == true && met.getHidden() == false) {
			setPrimaryMetStation(met.getId());
		}

		if (met.getHidden() == true || (met.getEnabled() != null && met.getEnabled() == false)) {
			//TODO we do this because sometimes a race condition occurs and alarms remain active. We should replace this with a synchronized block  
			final MetStation metForCloseAlarms = met;
			Runnable closeAlarmsRun = new Runnable() {

				@Override
				public void run() {
					alarmManager.closeMetAlarms(metForCloseAlarms);
				}
				
			};
			closeAlarmsRun.run();
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			scheduler.schedule(closeAlarmsRun, 30, TimeUnit.SECONDS);
		}

		EncryptionUtils.decryptCredentials(met);
		if ((isAddNew || isEarthNetworkStationChanged) && isEarthNetwork) {
			saveEarthNetworkMetAverage(met);
		}

		return unitsManager.toCustom(mapOrNewInstance(met, MetStationDTO.class), getCurrentSite().getId(),getCurrentUser().getId());
	}

	@Secured("ROLE_SITE_ADMIN")
	protected <T extends HasId> T saveSensorInterface(SensorInterfaceDTO data) throws FieldValidationException {
		if (!getCurrentUser().isSiteAdmin()) {
			throw new FieldValidationException("You are not allowed to edit sensor interfaces");
		}
		
		unitsManager.toInternal(data, getCurrentSite().getId(),getCurrentUser().getId());

		SensorInterface si;

		boolean isAddNew = (data.getId() == null);

		if (isAddNew) {
			si = mapper.map(data, SensorInterface.class);
			si.setCreatedDate(new Date());
			si.setSite(getCurrentSite());
		} else {
			si = dataSourceManager.getSensorInterfaceById(data.getId());
			si.setModifiedDate(new Date());
			mapper.map(data, si);
			// If we don't do this, the Daq fields will get overwritten by the
			// fields belonging to the Daq object on the ports, because the ORM
			// uses the same reference for both
			mapper.map(data.getDaq(), si.getDaq());
		}
		
		EncryptionUtils.encryptCredentials(si);
		si = dataSourceManager.saveSensorInterface(si);
		
		if(Boolean.FALSE.equals(si.getEnabled())) {
			dataSourceManager.disableRaeForSensorInterface(si);
			final SensorInterface interfaceForCloseAlarms = si;
			//TODO we do this because sometimes a race condition occurs and alarms remain active. We should replace this with a synchronized block
			Runnable closeAlarmsRun = new Runnable() {

				@Override
				public void run() {
					for(SensorRae rae : dataSourceManager.getSensorRaes(interfaceForCloseAlarms)) {
						alarmManager.closeSensorRaeAlarms(rae, true);
					}
					for(Sensor sensor : dataSourceManager.getSensors(interfaceForCloseAlarms)) {
						alarmManager.closeSensorAlarms(sensor);
					}
				}
				
			};
			closeAlarmsRun.run();
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			scheduler.schedule(closeAlarmsRun, 30, TimeUnit.SECONDS);
		}
		
		return unitsManager.toCustom(mapOrNewInstance(si, SensorInterfaceDTO.class), getCurrentSite().getId(),getCurrentUser().getId());
	}
	
	
	protected <T extends HasId> T saveSensorRae(SensorRaeDTO data) throws FieldValidationException {
		if (!getCurrentUser().isSiteAdmin()) {
			throw new FieldValidationException("You are not allowed to edit RAE sensors");
		}
		
		unitsManager.toInternal(data, getCurrentSite().getId(),getCurrentUser().getId());

		SensorRae rae;

		boolean isAddNew = (data.getId() == null);

		if (isAddNew) {
			rae = mapper.map(data, SensorRae.class);
			rae.setCreatedDate(new Date());
			rae.setSite(getCurrentSite());
		} else {
			rae = dataSourceManager.getSensorRaeById(data.getId());
			rae.setModifiedDate(new Date());
			
			Integer sensorLabelRotation = null;
			boolean labelRotationChanged = false;
			if (rae.getLabelRotation() != null && !rae.getLabelRotation().equals(data.getLabelRotation()) ||
					rae.getLabelRotation() == null && data.getLabelRotation() != null) {
				sensorLabelRotation = data.getLabelRotation();
				labelRotationChanged = true;
			}
			
			Point sensorPosition = null;
			Date currentDate = new Date();
			if(!data.isGpsEnabled() && rae.getLocation()!=null) {
				PointDTO oldPosition = new PointDTO(rae.getLocation().getX(), rae.getLocation().getY());
				PointDTO newPosition = data.getLocation();
				if (!oldPosition.equals(newPosition)) {
					sensorPosition = new PointConverter().convertTo(newPosition, null);
					rae.setLastLocationChangeDate(currentDate);
				}
			}
			
			if (sensorPosition != null || labelRotationChanged ) {
				for(Sensor sensor : dataSourceManager.getSensorsByRae(rae)) {
					if (sensorPosition != null) {
						sensor.setLocation(sensorPosition);
						sensor.setLastLocationChangeDate(currentDate);
					}
					
					if (labelRotationChanged) {
						sensor.setLabelRotation(sensorLabelRotation);
					}
					
					dataSourceManager.saveSensor(sensor);
				}
			}
			mapper.map(data, rae);
		}
		
		rae = dataSourceManager.saveSensorRae(rae);

		return unitsManager.toCustom(mapOrNewInstance(rae, SensorRaeDTO.class), getCurrentSite().getId(),getCurrentUser().getId());
	}
	
	protected <T extends HasId> T saveSensorGroup(SensorGroupDTO data) throws FieldValidationException {
		if (!getCurrentUser().isSiteAdmin()) {
			throw new FieldValidationException("You are not allowed to edit sensors");
		}
		
		unitsManager.toInternal(data, getCurrentSite().getId(),getCurrentUser().getId());
		convertToInternalUnits(data.getAlarmValues());
		SensorGroup group;

		boolean isAddNew = (data.getId() == null);

		if(data.isSinglePath()) {
			saveSensorMixtureChemical(data);
		}
		
		if (isAddNew) {
			
			group = mapper.map(data, SensorGroup.class);
			group.setCreatedDate(new Date());
			group.setSite(getCurrentSite());
			
			group = dataSourceManager.saveSensorGroup(group);
			data.setId(group.getId());
			unitsManager.toCustom(data, getCurrentSite().getId(),getCurrentUser().getId());
			saveGroupNewSensors(data);
			
		} else {
			
			group = dataSourceManager.getSensorGroupById(data.getId());
			group.setModifiedDate(new Date());
			
			Integer sensorLabelRotation = null;
			boolean labelRotationChanged = false;
			if (group.getLabelRotation() != null && !group.getLabelRotation().equals(data.getLabelRotation()) ||
					group.getLabelRotation() == null && data.getLabelRotation() != null) {
				sensorLabelRotation = data.getLabelRotation();
				labelRotationChanged = true;
			}
			
			Point sensorPosition = null;
			Date currentDate = new Date();
			if(group.getLocation()!=null) {
				PointDTO oldPosition = new PointDTO(group.getLocation().getX(), group.getLocation().getY());
				PointDTO newPosition = data.getLocation();
				if (!oldPosition.equals(newPosition)) {
					sensorPosition = new PointConverter().convertTo(newPosition, null);
					group.setLastLocationChangeDate(currentDate);
				}
			}
			
			Boolean oldEnabled = group.getEnabled();
			Boolean newEnabled = data.getEnabled();
			 
			mapper.map(data, group);
			
			for(Sensor sensor : dataSourceManager.getSensorsByGroup(group)) {
				if (sensorPosition != null) {
					sensor.setLocation(sensorPosition);
					sensor.setLastLocationChangeDate(currentDate);
				}
				if (labelRotationChanged) {
					sensor.setLabelRotation(sensorLabelRotation);
				}
				sensor.setSensorInterface(group.getSensorInterface());
				sensor.setHeight(data.getHeight());
				sensor.setReflectorLocation(new PointConverter().convertTo(data.getReflectorLocation(), null));
				sensor.setDistance(data.getDistance());
				if(data.isSinglePath()) {
					sensor.setName( sensor.getChemical().getName());
					sensor.setMixtureChemical(mapper.map(data.getChemical(),Chemical.class));
				}
				if(data.isMultiPath()) {
					sensor.setChannelTag(group.getChannelTag());
					sensor.setChemical(group.getChemical());
					sensor.setAlarmValues(group.getAlarmValues());
					sensor.setUnitConvertor(group.getUnitConvertor());
					sensor.setName( data.getChemical().getName() + " #" + sensor.getChannelTag());
				}
				if(!oldEnabled.equals(newEnabled)) {
					sensor.setEnabled(newEnabled);
				}
				sensor.setHidden(group.getHidden());
				if (sensor.getHidden() == true || (sensor.getEnabled() != null && sensor.getEnabled() == false)) {
					closeSensorAlarms(sensor);
				}
				dataSourceManager.saveSensor(sensor);
			}
			
			group = dataSourceManager.saveSensorGroup(group);			
			unitsManager.toCustom(data, getCurrentSite().getId(),getCurrentUser().getId());
			saveGroupNewSensors(data);
		}

		return unitsManager.toCustom(mapOrNewInstance(group, SensorGroupDTO.class), getCurrentSite().getId(),getCurrentUser().getId());
	}

	@Override
	public String getSensorMixtureChemicalName(List<ChemicalDTO> chemicals) {
		if(chemicals.isEmpty()) {
			return null;
		}
		if(chemicals.size()==1) {
			return chemicals.get(0).getName();
		}
		List<Integer> chemicalIdList = new ArrayList<>();
		for(ChemicalDTO chemical : chemicals) {
			chemicalIdList.add(chemical.getId());
		}
		String mixtureChemicalIds = ChemicalUtils.getSensorMixtureChemicalIds(chemicalIdList);
		ChemicalSite mixture = chemicalsManager.findSensorMixture(mixtureChemicalIds, getCurrentSite());
		if(mixture==null) {
			String name = "Mixture";
			for(ChemicalDTO chemical : chemicals) {
				name+= " " + chemical.getName();
			}
			return name;
		}else {
			return mixture.getChemical().getName();
		}
	}
	
	private void saveSensorMixtureChemical(SensorGroupDTO data) {
		if(data.getSensors().isEmpty()) {
			data.setChemical(null);
		}else if(data.getSensors().size() == 1 ) {
			data.setChemical(data.getSensors().get(0).getChemical());
		}else {
			String mixtureChemicalIds = getSensorMixtureChemicalIds(data);
			ChemicalSite mixture = chemicalsManager.findSensorMixture(mixtureChemicalIds, getCurrentSite());
			if(mixture == null) {
				List<MixtureChemicalDTO> mixtureChemicals = new ArrayList<>();
				String name = "Mixture";
				for(SensorDTO sen : data.getSensors()) {
					if(sen.getChemical()!=null) {
						MixtureChemicalDTO mixChem = new MixtureChemicalDTO();
						mixChem.setCompAmount(100.0/data.getSensors().size());
						mixChem.setCompChemical(mapper.map(chemicalsManager.findById(sen.getChemical().getId()),ChemicalDetailsDTO.class));
						mixtureChemicals.add(mixChem);
						name+= " " + sen.getChemical().getName();
					}
				}
				ChemicalDetailsDTO chem = ChemicalUtils.calcMixtureProps(mapper.mapAsList(mixtureChemicals,MixtureChemical.class), MixtureCompositionType.MASS_FRACTION);
				chem.setMixtureChemicals(mixtureChemicals);
				chem.setMixtureChemicalIds(mixtureChemicalIds);
				chem.setSensorMixture(true);
				chem.setName(name);
				chem.setEnabled(true);
				chem.setMixtureModelType(MixtureModelType.GAS);
				Chemical savedMixture = chemicalsManager.saveChemicalDetails(chem, getCurrentSite());
				data.setChemical(mapper.map(savedMixture,ChemicalDTO.class));
			}else {
				data.setChemical(mapper.map(mixture.getChemical(),ChemicalDTO.class));
			}
		}
		
		for(SensorDTO sen : data.getSensors()) {
			sen.setMixtureChemical(data.getChemical());
		}
	}
	
	private String getSensorMixtureChemicalIds(SensorGroupDTO data) {
		List<Integer> chemicalIdList = new ArrayList<>();
		for(SensorDTO sen : data.getSensors()) {
			if(sen.getChemical()!=null) {
				chemicalIdList.add(sen.getChemical().getId());
			}
		}
		return ChemicalUtils.getSensorMixtureChemicalIds(chemicalIdList);
	}
	


	private void saveGroupNewSensors(SensorGroupDTO data) throws FieldValidationException {
		for(SensorDTO sensor : data.getSensors()) {
			if(sensor.getId()<0) {
				sensor.setId(null);
				sensor.setSensorGroup(data);
				sensor.setSensorInterface(data.getSensorInterface());
				sensor.setLocation(data.getLocation());
				sensor.setLabelRotation(data.getLabelRotation());
				sensor.setReflectorLocation(data.getReflectorLocation());
				sensor.setDistance(data.getDistance());
				if(Boolean.FALSE.equals(data.getEnabled())) {
					sensor.setEnabled(false);
				}
				if(data.isMultiPath()) {
					sensor.setChannelTag(data.getChannelTag());
					sensor.setChemical(data.getChemical());
					sensor.setAlarmValues(data.getAlarmValues());
					sensor.setUnitConvertor(data.getUnitConvertor());
					sensor.setName(data.getChemical().getName() + " #" + sensor.getChannelTag());
				}
				if(data.isSinglePath()) {
					sensor.setName(sensor.getChemical().getName());	
				}
				sensor.setHeight(data.getHeight());
				saveSensor(sensor);
			}
		}
	}
	
	private void saveEarthNetworkMetAverage(MetStation met) {
		MetReading metReading = earthNetworkApiClient.getMetReading(met.getUsername(),  met.getEarthNetworkInfo().getApiStation().getApiProviderId(), met.getEarthNetworkInfo().getApiStation().getApiStationId());
		List<MetReading> readings = new ArrayList<MetReading>();
		readings.add(metReading);
		MetAverageProcessor metAverageProcessor = new MetAverageProcessor(met, metReading.getDateTaken(), readings, readings);
		MetData averageData = metAverageProcessor.getAverage();
		MetAverage average = new MetAverage();
		average.setMetData(averageData);
		average.setSamples(readings.size());
		average.setMetStation(met);
		average.setDateTaken(DateUtils.trimToMinutes(metReading.getDateTaken()));
		average.setLocation(met.getLocation());
		average.setType(MetAverageType.INSTANT);
		average.setApiStationId(met.getEarthNetworkInfo().getApiStation().getApiStationId());
		dataSourceManager.processMetAverage(average);
	}

	@Override
	public void enableMetStation(Integer metStationId, PointDTO position) throws RuntimeException {
		MetStation met = dataSourceManager.getMetStation(metStationId);
		if (Boolean.FALSE.equals(met.getEnabled()) && Config.MAX_ACTIVE_MET_PER_SITE <= dataSourceManager.countEnabledMetStationsPerSite(getCurrentSite().getId())){
			throw new RuntimeException(Labels.MAX_ACTIVE_METSTATIONS);
		}
		met.setLocation(new PointConverter().convertTo(position, null));
		met.setEnabled(true);
		dataSourceManager.save(met);
		setPrimaryMetStation(metStationId);
	}

	protected <T extends HasId> T saveDaq(DaqDTO data) {
		Daq daq;
		
		List<Site> oldSites = new ArrayList<Site>();
		if (data.getId() != null) {
			daq = daqManager.getDaq(data.getId());
			if (Boolean.TRUE.equals(daq.getDefaultDaq()) && !UserUtils.isSaferAdmin(getCurrentUser().getRole())) {
				throw new RuntimeException(Labels.CANT_EDIT_THIS_DAQ);
			}
			Boolean defaultDaq = daq.getDefaultDaq();
			oldSites = daq.getSites();
			mapper.map(data, daq);
			//client can't edit this flag
			data.setDefaultDaq(defaultDaq);
		} else {
			daq = mapper.map(data, Daq.class);
		}

		daq.setOrganization(getCurrentUser().getOrganization());
		daq = daqManager.save(daq);
		
		for(Site site : oldSites) {
			if(!daq.getSites().contains(site)) {
				List<MetStation> mets = dataSourceManager.getMetStationsForDaqAndSite(daq, site);
				for (MetStation met : mets) {
					met.setDaq(null);
					met.setPort(null);
					dataSourceManager.save(met);
				}
			}
		}
		return mapOrNewInstance(daq, DaqDTO.class);
	}

	protected <T extends HasId> T savePort(PortDTO data) {
		Port port;
		if (Boolean.TRUE.equals(data.getDaq().getDefaultDaq()) && !getCurrentUser().isSuperAdmin()) {
			throw new RuntimeException(Labels.CANT_EDIT_THIS_DAQ);
		}
		if (data.getId() != null) {
			port = daqManager.getPort(data.getId());
			mapper.map(data, port);
		} else {
			port = mapper.map(data, Port.class);
		}

		port = daqManager.save(port);
		return mapOrNewInstance(port, PortDTO.class);
	}
	
	protected <T extends HasId> T saveFeedback(FeedbackDTO data) {
		Feedback feedback;
		if (!UserUtils.isSaferAdmin(getCurrentUser().getRole())) {
			throw new RuntimeException("Must be Safer Admin");
		}
		
		feedback = feedbackManager.getFeedback(data.getId());
		feedback.setClosed(data.getClosed());
		

		feedback = feedbackManager.save(feedback);
		return mapOrNewInstance(feedback, FeedbackDTO.class);
	}
	
	protected <T extends HasId> T saveManualMet(ManualMetDataDTO data) {
		unitsManager.toInternal(data.getMetData(), getCurrentSite().getId(),getCurrentUser().getId());
		
		ManualMetData manualMet;
		User currentUser = getCurrentUser();
		Site currentSite = currentUser.getCurrentSite();
		
		if (data.getId() != null) {
			manualMet = dataSourceManager.getManualMetData(data.getId());
			mapper.map(data, manualMet);
		} else {
			manualMet = mapper.map(data, ManualMetData.class);
		}

		manualMet.setUser(currentUser);
		manualMet.setSite(currentSite);
		
		manualMet = dataSourceManager.save(manualMet);
		return mapOrNewInstance(manualMet, ManualMetDataDTO.class);
	}
	
	@SuppressWarnings("unchecked")
	protected <T extends HasId> T saveSiteSettings(SiteSettingsDTO data) {
		
		Site site = getCurrentSite();
		Integer userId = getCurrentUser().getId();
		
		if(data.getSiteId() !=null){
			if(!getCurrentUser().isSiteAdmin()){
				throw new RuntimeException("Operation not allowed");
			}
			site = siteManager.getSite(data.getSiteId());
			if(site==null){
				throw new RuntimeException("Data not found");
			}
			//userId = null;
		}
		
		unitsManager.toInternal(data.getAlarmLevelsData(), site.getId(),userId);
		unitsManager.toInternal(data.getOverpressureIsopleths(), site.getId(),userId);
		unitsManager.toInternal(data.getThermalIsopleths(), site.getId(),userId);
		
		SiteSettings siteSettings = null;
		
		boolean isEarthNetworkKeyChanged = true;
		if (data.getId() != null) {
			siteSettings = siteManager.getSiteSettings(data.getId());
			if((siteSettings.isCombustion()!=data.isCombustion()
					||siteSettings.isErg()!=data.isErg()
					||siteSettings.isFireAndExplosion()!=data.isFireAndExplosion()
					||siteSettings.isSalAndAbc()!=data.isSalAndAbc()
					||siteSettings.isDispersion()!=data.isDispersion())&&!getCurrentUser().isSaferAdmin()){
				throw new RuntimeException("You are not alowed to modify features license. Please contact SAFER Support.");
			}
			isEarthNetworkKeyChanged = !StringUtils.equals(siteSettings.getEarthNetworkUsername(), data.getEarthNetworkUsername());
			mapper.map(data, siteSettings);
		} else {
			siteSettings = mapper.map(data, SiteSettings.class);
		}
		
		siteSettings.setSite(site);
		
		siteSettings = siteManager.save(siteSettings);
		
		if(isEarthNetworkKeyChanged) {
			SecureCredentials creds = new SecureCredentials();
			creds.setUsername(getEarthNetworkKey());
			EncryptionUtils.encryptCredentials(creds);
			dataSourceManager.setMetStationCredentialsForSiteAndType(creds, site.getId(), MetStationType.EARTH_NETWORK_API);
		}
		
		SiteSettingsDTO result = mapOrNewInstance(siteSettings, SiteSettingsDTO.class);
		unitsManager.toCustom(result.getAlarmLevelsData(), site.getId(),userId);
		unitsManager.toCustom(result.getOverpressureIsopleths(), site.getId(),userId);
		unitsManager.toCustom(result.getThermalIsopleths(), site.getId(),userId);
		return (T) result;
	}

	@SuppressWarnings("unchecked")
	protected <T extends HasId> T saveOrganizationSettings(OrganizationDTO data) {
		
        
		Organization organization=null;
		if(data.getId() !=null){
			if(!getCurrentUser().isOrganizationAdmin()&&!getCurrentUser().isSaferAdmin()&&!getCurrentUser().isSuperAdmin()){
				throw new RuntimeException("Operation not allowed");
			}
			organization =   organizationManager.getOrganization(data.getId());
			if(organization==null){
				throw new RuntimeException("Data not found");
			}
			//userId = null;
		}
		
		if(data.getResetPasswordHour()==null){
			throw new RuntimeException("Invalid input for 'Minimum time required between password change or reset'");
		}else{
			
			if(data.getResetPasswordHour()<0||data.getResetPasswordHour()>72){
				throw new RuntimeException("Minimum time required between password change or reset must be 0-72");
			}
		}
		
		if(data.getNumberOfPasswords()==null){
			throw new RuntimeException("Invalid input for 'Number of previous passwords that cannot be reused'");
		}else{
			
			if(data.getNumberOfPasswords()<1||data.getNumberOfPasswords()>12){
				throw new RuntimeException("Number of previous passwords that cannot be reused must be 1-12");
			}
		}		
		organization.setForceExpiredPassword(data.getForceExpiredPassword());
		organization.setResetPasswordHour(data.getResetPasswordHour());
		organization.setNumberOfPasswords(data.getNumberOfPasswords());
        organizationManager.save(organization);
		return (T) data;
	}	
	
	
	
	protected <T extends HasId> T savePoi(PointOfInterest poi){
		Date d = new Date();
		if(poi.getCreatedDate()==null){
			poi.setCreatedDate(d);
		}
		poi.setModifiedDate(d);
		if(StringUtils.isBlank(poi.getSourceType()) && StringUtils.isBlank(poi.getSourceId())){
			poi.setSourceId("manual_"+d.getTime()+((int)(Integer.MAX_VALUE*Math.random())) );
			poi.setSourceType("manual");
		}
		poi.setSite(getCurrentUser().getCurrentSite());
		poi = poisManager.save(poi);
		return mapOrNewInstance(poi, PointOfInterestDTO.class);
	}
	
	protected <T extends HasId> T savePoi(PointOfInterestDTO data) throws FieldValidationException {
		/*if (EarthCalc.getDistance(data.getLocation().asGeoCalcPoint(), 
				new PointDTO(getCurrentSite().getLatitude(), getCurrentSite().getLongitude()).asGeoCalcPoint()) > getCurrentSite().getRadius()) {
			throw new FieldValidationException("Location outside site range");
		}*/
		
		if(data.getDefaultACH()==null) {
			data.setDefaultACH(1.5d);
		}
		PointOfInterest poi;
		if (data.getId() != null) {
			poi = poisManager.getPoiById(data.getId());
			mapper.map(data, poi);
		} else {
			poi = mapper.map(data, PointOfInterest.class);
		}
		
		return savePoi(poi);
	}

	protected <T extends HasId> T clonePoi(int id, PointDTO position, Integer groupId) throws FieldValidationException {
		PointOfInterest poi = poisManager.getPoiById(id);
		poi.setSite(getCurrentUser().getCurrentSite());
		poi.setId(null);
		poi.setCreatedDate(new Date());
		if(groupId!=null && poi.getGroupId()==null){
			poi.setGroupId(groupId);
		}
		
		poi.setLocation(mapper.map(position, Point.class));
		poi.setSourceId(null);
		poi.setSourceType(null);
		if (!poi.getName().startsWith("Clone of")) {
			poi.setName("Clone of " + poi.getName());
		}
		savePoi(poi);
		return mapOrNewInstance(poi, PointOfInterestDTO.class);
	}
	
	protected <T extends HasId> T cloneSensor(int id, PointDTO position) throws FieldValidationException {
		Sensor sensor = dataSourceManager.getSensor(id);
		sensor.setId(null);
		sensor.setCreatedDate(new Date());
		sensor.setModifiedDate(null);
		sensor.setLocation(mapper.map(position, Point.class));
		if (!sensor.getName().startsWith("Clone of")) {
			sensor.setName("Clone of " + sensor.getName());
		}
		sensor = dataSourceManager.saveSensor(sensor);
		return mapOrNewInstance(sensor, SensorDTO.class);
	}
	
	@Transactional
	protected SiteDTO saveSite(SiteDTO siteDto) throws FieldValidationException {

		if (!validator.validate(siteDto).isEmpty()) {
			throw new FieldValidationException("Invalid data");
		}

		final boolean isAddNew = siteDto.getId() == null;
		User user = getCurrentUser();
		
		siteDto = unitsManager.toInternal(siteDto, getCurrentSite().getId(),getCurrentUser().getId());
		if (!isAddNew){
			Site existingSite = siteManager.getSite(siteDto.getId());
			//Organization Admin should not be able to change site center
			if (!user.isSaferAdmin() && existingSite!=null){ 
				siteDto.setLatitude(existingSite.getLatitude());
				siteDto.setLongitude(existingSite.getLongitude());
			}
		} 
		// TODO: validate that a user cannot save sites in another organization

		Site site = mapper.map(siteDto, Site.class);
		site = siteManager.save(site);

		if (isAddNew) {
			// Units defaultUnits = unitsManager.getDefaultUnits();
			// List<UnitSettingWithGroupDTO> unitsList = new
			// ArrayList<UnitSettingWithGroupDTO>();
			// unitsList.addAll(defaultUnits.getUnitSettings());
			// unitsManager.save(unitsList, site.getId());

			if (!user.getSites().contains(site) && site.getOrganization().equals(user.getOrganization())) {
				user.getSites().add(site);
			}
			user = userManager.save(user);
			user.setModifiedBy(getCurrentUser().getId());
			saferContext.setCurrentUser(user);

			if(!properties.getDefaultDaqs().isEmpty()) {
				List<Daq> daqs = daqManager.getDaqs(site.getOrganization());
				for(Daq daq : daqs) {
					for (DaqDTO defDaq:properties.getDefaultDaqs())
					if(defDaq.getDaqKey().equals(daq.getDaqKey())) {
						daq.getSites().add(site);
						daqManager.save(daq);
						break;
					}
				}
			}
		}

		SiteDTO ret = mapOrNewInstance(site, SiteDTO.class);
		ret = unitsManager.toCustom(ret, getCurrentSite().getId(),getCurrentUser().getId());
		
		return ret;
	}

	protected EmissionSourceDTO saveEmissionSource(EmissionSourceDTO emissionDTO) throws FieldValidationException {
		if (EarthCalc.getDistance(emissionDTO.getLocation().asGeoCalcPoint(), 
				new PointDTO(getCurrentSite().getLatitude(), getCurrentSite().getLongitude()).asGeoCalcPoint()) > getCurrentSite().getRadius()) {
			throw new FieldValidationException("Location outside site range");
		}
//		if (!validator.validate(emissionDTO, Default.class, com.safer.one.gwt.shared.validation.EmissionSource.class, ValidationUtils.getEmissionSourceValidationClass(emissionDTO.getType())).isEmpty()) {
//			throw new FieldValidationException("Invalid data");
//		}

		emissionDTO = unitsManager.toInternal(emissionDTO, getCurrentSite().getId(),getCurrentUser().getId());
		EmissionSource em;
		if (emissionDTO.getId() != null) {
			em = emissionManager.getEmissionSource(emissionDTO.getId());
			mapper.map(emissionDTO, em);
		} else {
			em = mapper.map(emissionDTO, EmissionSource.class);
		}

		em.setSite(getCurrentUser().getCurrentSite());

		em = emissionManager.save(em);

		return unitsManager.toCustom(mapper.map(em, emissionDTO.getClass()), getCurrentSite().getId(),getCurrentUser().getId());
	}
	
	private void convertToInternalUnits(SensorAlarmValuesDTO alarms) {
		if(alarms!=null) {
			alarms.setAlarmLev1(getUnits().toInternalAllowNull(UnitParam.IDP_SENSOR_READING, alarms.getAlarmLev1()));
			alarms.setAlarmLev2(getUnits().toInternalAllowNull(UnitParam.IDP_SENSOR_READING, alarms.getAlarmLev2()));
			alarms.setMinLevel(getUnits().toInternalAllowNull(UnitParam.IDP_SENSOR_READING, alarms.getMinLevel()));
			alarms.setDeadband(getUnits().toInternalAllowNull(UnitParam.IDP_SENSOR_READING, alarms.getDeadband()));
			alarms.setSatuLev(getUnits().toInternalAllowNull(UnitParam.IDP_SENSOR_READING, alarms.getSatuLev()));
		}
	}

	protected SensorDTO saveSensor(SensorDTO sensorDTO) throws FieldValidationException {
		if (sensorDTO.getLocation()!=null && EarthCalc.getDistance(sensorDTO.getLocation().asGeoCalcPoint(), 
				new PointDTO(getCurrentSite().getLatitude(), getCurrentSite().getLongitude()).asGeoCalcPoint()) > getCurrentSite().getRadius()) {
			throw new FieldValidationException("Location outside site range");
		}

		sensorDTO = unitsManager.toInternal(sensorDTO, getCurrentSite().getId(),getCurrentUser().getId());
		SensorAlarmValuesDTO alarms = sensorDTO.getAlarmValues();
		if(!SensorType.isCustomUnitType(sensorDTO.getType())) {
			convertToInternalUnits(alarms);
		}
		
		Sensor sensor;
		if (sensorDTO.getId() != null) {
			sensor = dataSourceManager.getSensor(sensorDTO.getId());
			boolean wasDisabled = Boolean.FALSE.equals(sensor.getEnabled());
			if(sensor.getLocation()!=null) {
				PointDTO oldPosition = new PointDTO(sensor.getLocation().getX(), sensor.getLocation().getY());
				PointDTO newPosition = sensorDTO.getLocation();
				if (!oldPosition.equals(newPosition)) {
					sensor.setLastLocationChangeDate(new Date());
				}
			}
			
			if(sensorDTO.getSensorGroup()!=null) {
				SensorGroupDTO group = sensorDTO.getSensorGroup();
				if(group.isSinglePath()) {
					sensorDTO.setName(sensorDTO.getChemical().getName());
				}
				if(group.isMultiPath()) {
					sensorDTO.setName(sensorDTO.getChemical().getName() + " #" + sensor.getChannelTag());
				}
			}
			boolean alarmValuesChanged = !Objects.equals(sensor.getAlarmValues().getDeadband(), alarms.getDeadband())
			 							|| !Objects.equals(sensor.getAlarmValues().getAlarmLev1(), alarms.getAlarmLev1())
			 							|| !Objects.equals(sensor.getAlarmValues().getAlarmLev2(), alarms.getAlarmLev2())
			 							|| !Objects.equals(sensor.getAlarmValues().getMinLevel(), alarms.getMinLevel())
			 							|| !Objects.equals(sensor.getAlarmValues().getSatuLev(), alarms.getSatuLev());

			mapper.map(sensorDTO, sensor);
			if(!sensor.isAlarmValuesSetByUser() &&  alarmValuesChanged) {
				sensor.setAlarmValuesSetByUser(true);
			}
			sensor.setModifiedDate(new Date());
			if(wasDisabled && Boolean.TRUE.equals(sensor.getEnabled()))  {
				sensor.setEnabledDate(new Date());
			}
		} else {
			sensor = mapper.map(sensorDTO, Sensor.class);
			sensor.setAlarmValuesSetByUser(true);
			sensor.setCreatedDate(new Date());
		}

		if (sensor.getHidden() == true || (sensor.getEnabled() != null && sensor.getEnabled() == false)) {
			closeSensorAlarms(sensor);
		}
		
		sensor.setSite(getCurrentUser().getCurrentSite());
		sensor = dataSourceManager.saveSensor(sensor);
		
		return unitsManager.toCustom(mapper.map(sensor, sensorDTO.getClass()), getCurrentSite().getId(),getCurrentUser().getId());
	}
	
	
	private void closeSensorAlarms(final Sensor sensorForCloseAlarms) {
		//TODO we do this because sometimes a race condition occurs and alarms remain active. We should replace this with a synchronized block
		Runnable closeAlarmsRun = new Runnable() {

			@Override
			public void run() {
				alarmManager.closeSensorAlarms(sensorForCloseAlarms);
			}
			
		};
		closeAlarmsRun.run();
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		scheduler.schedule(closeAlarmsRun, 30, TimeUnit.SECONDS);
	}
	
	protected ScenarioRunDTO runScenario(ScenarioRunDTO scenarioRunDto,final  User user, final Site site) throws FieldValidationException {
		
		if (!user.isRealtime()){
			throw new FieldValidationException("You are not allowed to run scenarios");
		}
		
		if (scenarioRunDto.getScenarioType() != ScenarioType.SAL && EarthCalc.getDistance(scenarioRunDto.getLocation().asGeoCalcPoint(), 
				new PointDTO(site.getLatitude(), site.getLongitude()).asGeoCalcPoint()) > site.getRadius()) {
			throw new FieldValidationException("Location outside site range");
		}
		
		final ScenarioType scenType = scenarioRunDto.getScenarioType();
		if(ScenarioUtils.licenseDoesNotAlow(scenType, siteManager.getSiteSettings(getCurrentSite()))){
			throw new RuntimeException("Your license doesn't alow running "+scenType.getDisplayName());
		}
		
		PointDTO location = scenarioRunDto.getLocation();
		scenarioRunDto = unitsManager.toInternal(scenarioRunDto, site.getId(),user.getId());

		if(scenarioRunDto.getManualMetData() != null) {
			recalculateScenarioMetData(scenarioRunDto.getReleaseTime(), scenarioRunDto.getLocation(), scenarioRunDto.getManualMetData(), user, site);
			for(ScenarioMetDataDTO metData : scenarioRunDto.getManualMetData()) {
				unitsManager.toInternal(metData.getMetData(), site.getId(),user.getId());	
			}
		}
		
		ScenarioRun originalSr = null;
		if(scenarioRunDto.getId()!=null && scenarioRunDto.getId()>=0){
			originalSr = scenarioManager.getScenarioRun(scenarioRunDto.getId());
		}
		
		ScenarioRun sr;
		scenarioRunDto.setId(null);
		final boolean runUnderDiferentUser = (originalSr!=null && !user.getId().equals(originalSr.getUser().getId()));
		final boolean newScenarioOrRunUnderDiferentUser = (scenarioRunDto.getGroupId()==null || runUnderDiferentUser);
		
		Chemical chemical = chemicalsManager.findById(scenarioRunDto.getChemical().getId());
		sr = mapper.map(scenarioRunDto, ScenarioRun.class);
		sr.setScenario(mapper.map(scenarioRunDto, Scenario.class));
		sr.getScenario().setEmissionSource(mapper.map(scenarioRunDto, EmissionSource.class));
		sr.getScenario().getEmissionSource().setSite(site);
		sr.getScenario().getEmissionSource().setChemical(chemical);
		
		if (!scenarioRunDto.getScenarioType().equals(ScenarioType.ABC) && !scenarioRunDto.getScenarioType().equals(ScenarioType.SAL)) {
			//abc and sal receive sensors from client. For the rest of the scenario types I save the sensors here. 
			double minSenValue = scenarioRunDto instanceof ScenarioIsoplethDTO ? ((ScenarioIsoplethDTO)scenarioRunDto).getLow()/100 : 0;
			SensorAveragesWrapperDTO wrapper = getSensorInputAverages(scenarioRunDto.getReleaseTime(), scenarioRunDto.getChemical(), 
					minSenValue, true, site);
			sr.setSensors(mapper.mapAsList(wrapper.getSensors().values(), SensorModeling.class));
		}
		for (SensorModeling sm:sr.getSensors()){
			if (sm.getCurrentAverage() != null && sm.getCurrentAverage().getDateTaken() != null) {
				sm.getCurrentAverage().setDateTaken(DateUtils.convertFromTimezoneToUtc(sm.getCurrentAverage().getDateTaken(), site.getTimeZone()));
			}
		}

		sr.setCreatedDate(new Date());
		sr.setModifiedDate(new Date());
		sr.setUuid((long)(Math.random()*Long.MAX_VALUE));
		sr.setUser(user);
		if(newScenarioOrRunUnderDiferentUser){
			sr.setGroupId(null);
		}else{
			sr.setGroupId(scenarioRunDto.getGroupId());
		}
		if (sr.getReleaseTime() != null) {
			sr.setReleaseTime(DateUtils.convertFromTimezoneToUtc(sr.getReleaseTime(), site.getTimeZone()));
		}else{
			throw new RuntimeException("Release time can't be empty");
		}
		//sr.setVersion(sr.getVersion()+1);
		sr.setScenarioOutputDTO(new ScenarioOutDTO());
		
		sr = scenarioManager.saveScenarioRun(sr);
		String warnings = null;
		try {
			warnings  = modelingManager.runScenario(sr, site, user);
		} catch (Exception e) {
			sr.setHidden(true);
			e.printStackTrace();
		    throw new RuntimeException(e.getMessage() != null ? e.getMessage() : "Error running modeling");
		}finally {
			if(newScenarioOrRunUnderDiferentUser){ 
				sr.setGroupId(sr.getId());
				if(runUnderDiferentUser){
					cloneEventPlaces(originalSr.getGroupId(), sr.getGroupId(), site);
					cloneEventZones(originalSr.getGroupId(), sr.getGroupId(), site);
				}
			}
			sr.setModelWarnings(warnings);
		    sr = scenarioManager.saveScenarioRun(sr);
		}
		
		if (sr.getReleaseTime() != null) {
			sr.setReleaseTime(DateUtils.convertToTimezone(sr.getReleaseTime(), site.getTimeZone()));
		}
		
		scenarioRunDto = unitsManager.toCustom(mapper.map(sr, scenarioRunDto.getClass()), site.getId(),user.getId());
		if(scenarioRunDto.getManualMetData() != null) {
			for(ScenarioMetDataDTO metData : scenarioRunDto.getManualMetData()) {
				unitsManager.toCustom(metData.getMetData(), site.getId(),user.getId());	
			}
		}
		
		if(ScenarioType.POOL_FIRE.equals(sr.getScenarioType())){
			scenarioRunDto.setScenarioOut(sr.getScenarioOutputDTO());
		}
		
		User u = sr.getUser(); //TODO cristi see how we can make sites per user lazzy loading
		u.setOrganization(null);
		u.setSites(null);
		u.setLastSite( null);
		u.setCurrentSite(null);
		scenarioRunDto.setUser(mapper.map(u, UserDTO.class));
		scenarioRunDto.setLocation(location); 
		return scenarioRunDto;
	}
	
	protected EmissionSourceDTO saveScenario(EmissionSourceDTO emDTO) throws FieldValidationException {
		emDTO = unitsManager.toInternal(emDTO, getCurrentSite().getId(),getCurrentUser().getId());
		
		boolean saveEmissionSource = false;
		EmissionSource clientES = null;
		if (emDTO instanceof ScenarioRunDTO && ((ScenarioRunDTO)emDTO).getEmissionSourceId() != null) {
			clientES = emissionManager.getEmissionSource(((ScenarioRunDTO)emDTO).getEmissionSourceId());
		}
		Scenario scenario;
		if (emDTO.getId() != null) {
			scenario = scenarioManager.getScenario(emDTO.getId());
			//scenario user is deleted when mapping
			User user = scenario.getUser();
			
			mapper.map(emDTO, scenario);
			if (scenario.getEmissionSource().isScenarioHelper()) {
				Integer esId = scenario.getEmissionSource().getId();
				mapper.map(emDTO, scenario.getEmissionSource());
				scenario.getEmissionSource().setId(esId);
				scenario.getEmissionSource().setAutoName();
				saveEmissionSource = true;
			} else {
				saveEmissionSource = updateScenarioRunFieldsOnEmissionSource(emDTO, scenario);
			}
			scenario.setUser(user);
			scenario.setModifiedDate(new Date());
		} else {
			scenario = mapper.map(emDTO, Scenario.class);

			if (clientES == null) {
				scenario.setEmissionSource(mapper.map(emDTO, EmissionSource.class));
				scenario.getEmissionSource().setSite(getCurrentSite());
				scenario.getEmissionSource().setAutoName();
				saveEmissionSource = true;
			} else { 
				//new scenario started from an emission source. We will not save the emission source
				scenario.setEmissionSource(clientES);
				saveEmissionSource = updateScenarioRunFieldsOnEmissionSource(emDTO, scenario);
			}
			scenario.setSiteId(getCurrentSite().getId());
			scenario.setUser(getCurrentUser());
			scenario.setCreatedDate(new Date());
		}
		
		scenario = scenarioManager.saveScenario(scenario, saveEmissionSource);
		
		emDTO = unitsManager.toCustom(mapper.map(scenario, emDTO.getClass()), getCurrentSite().getId(),getCurrentUser().getId());
		return emDTO;
	}

	public boolean importScenario(String dataFile, String logFile, List<Integer> selectedIds, Site site) {

		PrintWriter pw = null;
		
		try {
			
			pw = new PrintWriter(logFile);
		
			ObjectMapper omap = new ObjectMapper();
			
			Scenario[] data = new Scenario[0];
			
			pw.println("Reading data file...");
			
			try {
				data = omap.readValue(new File(dataFile), Scenario[].class);
			} catch (IOException e) {
				pw.printf("ERROR: Failed to read/access data file. (%s)\r\n", e.getMessage());
				return false;
			}				
			pw.printf("Data file successfully read, total records %d, selected records %d\r\n", data.length, selectedIds.size());
				
			Scenario scenario = null;
			int rtId = 0;
			boolean changeToWater = false;
			Chemical chem = null;
			String rtChemName = "";
			
			for (int i = 0; i < data.length; i++) {
				
				scenario = data[i];
				rtId = scenario.getId();
				if (!selectedIds.contains(rtId)) //skip if not selected
					continue;
				
				changeToWater = false;
				
				scenario.setId(null); //reset id				
				scenario.getEmissionSource().setSite(site);
				scenario.getEmissionSource().setAutoName();
				
				//set EmissionSource chemical using safer no.
				chem = scenario.getEmissionSource().getChemical();
				if (chem.getSaferNo() == 9300) {
					//Obsolete predefined solution, skip
					pw.printf("Skipped, uses obsolete chemical:%s (%s) [RT=%d]\r\n", scenario.getName(), chem.getName(), rtId);
					continue;
				}										
				else if (chem.getSaferNo() == 9000 || chem.getSaferNo() == 9100 ||
					chem.getSaferNo() == 9200 || chem.getSaferNo() == 9400 ||
					chem.getSaferNo() == 9700 || chem.getSaferNo() == 9800 ||
					chem.getSaferNo() <= 0) {
					
					changeToWater = true;
					chem.setSaferNo(20); //set to water, inform user
				}
				else if (chem.getSaferNo() == 9500)
					chem.setSaferNo(9501); //use new number
				else if (chem.getSaferNo() == 9600)
					chem.setSaferNo(9601); //use new number
				
				rtChemName = chem.getName();
				
				chem = chemicalsManager.findBySaferNo(scenario.getEmissionSource().getChemical().getSaferNo());
				scenario.getEmissionSource().setChemical(chem);				
					
				scenario.setSiteId(site.getId());
				scenario.setUser(getCurrentUser());
				scenario.setCreatedDate(new Date());
				
				//Set isopleth
				ChemicalDetailsDTO chemDetails = chemicalsManager.getChemicalDetails(chem.getId(), site);
				
				scenario.setIsoplethName(ConcentrationIsoplethsType.TOXICITY.toString());
				
				ConcentrationIsoplethsDTO concIso = chemDetails.getConcentrationIsopleths();
				scenario.setLow(concIso.getLow());
				scenario.setLowLabel(concIso.getLowLabel());
				scenario.setMedium(concIso.getMedium());
				scenario.setMediumLabel(concIso.getMediumLabel());
				scenario.setHigh(concIso.getHigh());
				scenario.setHighLabel(concIso.getHighLabel());

				scenario.setAveragingTime(concIso.getAveragingTime());
				
				scenario = scenarioManager.saveScenario(scenario, true);
				
				if (changeToWater)
					    pw.printf("Added: %s [RT=%d, S1=%d]] **change chemical to \"%s\"\r\n", 
							             scenario.getName(), rtId, scenario.getId(), rtChemName);
				else
					pw.printf("Added: %s [RT=%d, S1=%d]\r\n", scenario.getName(), rtId, scenario.getId());
			}
			
			pw.println("Import completed.");
			
		//} catch (FileNotFoundException e1) {
			} catch (FileNotFoundException e1) {

			return false;
		}
		finally {
			if (pw != null)
				pw.close();
		}
		
		return true;			
	}

	
	private boolean updateScenarioRunFieldsOnEmissionSource(EmissionSourceDTO scenarioDTO, Scenario scenario) {
		EmissionSource savedES = scenario.getEmissionSource();
		//scenario fields are saved on emission source. we have to save event if the emission source is defined by the user
		if (scenario.getScenarioType() == ScenarioType.TANK_RELEASE) {
			Double inputPipeLength = ((ScenarioRunTankReleaseDTO)scenarioDTO).getPipeLength();
			Double inputPipeDiameter = ((ScenarioRunTankReleaseDTO)scenarioDTO).getPipeDiameter();
			if ((savedES.getPipeLength() != null && !savedES.getPipeLength().equals(inputPipeLength) ||
					savedES.getPipeLength() == null && inputPipeLength != null) ||
					(savedES.getPipeDiameter() != null && !savedES.getPipeDiameter().equals(inputPipeDiameter) ||
					savedES.getPipeDiameter() == null && inputPipeDiameter != null )) {
				savedES.setPipeLength(inputPipeLength);
				savedES.setPipeDiameter(((ScenarioRunTankReleaseDTO)scenarioDTO).getPipeDiameter());
				return true;
			}
		}
		return false;
	}

	protected EmissionSourceDTO cloneEmissionSource(int id, PointDTO position, Boolean pasteWithScenarios) throws FieldValidationException {
		EmissionSource em = emissionManager.getEmissionSource(id);
		List<Scenario> emScenarios = null;
		if (Boolean.TRUE.equals(pasteWithScenarios)) {
			emScenarios = scenarioManager.getScenarios(em);
		}
		
		if (em == null) {
			throw new FieldValidationException("Invalid emission source");
		}
		
		em.setSite(getCurrentUser().getCurrentSite());
		em.setId(null);
		em.setCreatedDate(new Date());
		em.setLocation(mapper.map(position, Point.class));
		if (!em.getName().startsWith("Clone of")) {
			em.setName("Clone of " + em.getName());
		}
		em = emissionManager.save(em);

		if (emScenarios != null) {
			for (Scenario s:emScenarios) {
				s.setId(null);
				s.getEmissionSource().setId(em.getId());
				if (!s.getName().startsWith("Clone of")) {
					s.setName("Clone of " + s.getName());
				}
				scenarioManager.saveScenario(s, false);
			}
		}
		
		return mapper.map(em, EmissionSourceDTO.class);
	}
	
	@Transactional
	protected TipOfTheDayDTO saveTipOfTheDay(TipOfTheDayDTO tipDto) throws FieldValidationException {
		if (!(getCurrentUser().isSaferAdmin()) ) {
			throw new FieldValidationException("You are not allowed to edit this Quick Tip");
		}
		if (!validator.validate(tipDto).isEmpty()) {
			throw new FieldValidationException("Invalid data");
		}

		TipOfTheDay org;
		if (tipDto.getId() != null) {
			org = tipOfTheDayManager.getTipOfTheDay(tipDto.getId());
			mapper.map(tipDto, org);
		} else {
			org = mapper.map(tipDto, TipOfTheDay.class);
		}
		
		if(org.isForceOnlyMe()){
			tipOfTheDayManager.cleanForceOnlyMe();
		}

		org = tipOfTheDayManager.save(org);
		
		return mapper.map(org, TipOfTheDayDTO.class);
	}
	
	protected OrganizationDTO saveOrganization(OrganizationDTO organizationDto) throws FieldValidationException {
		if (!(getCurrentUser().isSaferAdmin() || getCurrentUser().isOrganizationAdmin()
				&& organizationDto.getId().equals(getCurrentUser().getOrganization().getId()))) {
			throw new FieldValidationException("You are not allowed to edit this organization");
		}
		if (!validator.validate(organizationDto).isEmpty()) {
			throw new FieldValidationException("Invalid data");
		}

		boolean isAddNew = organizationDto.getId() == null;
		
		Organization org;
		if (organizationDto.getId() != null) {
			org = organizationManager.getOrganization(organizationDto.getId());
			mapper.map(organizationDto, org);
		} else {
			org = mapper.map(organizationDto, Organization.class);
		}

		org = organizationManager.save(org);
		
		if(isAddNew && !properties.getDefaultDaqs().isEmpty()) {
			for (DaqDTO defDaq:properties.getDefaultDaqs()) {
				Daq daq = new Daq();
				daq.setName(defDaq.getName());
				daq.setDefaultDaq(true);
				daq.setDaqKey(defDaq.getDaqKey());
				daq.setOrganization(org);
				daq = daqManager.save(daq);
			}
		}
		
		SaferContext.updateOrganization(org);
		return mapper.map(org, OrganizationDTO.class);
	}

	protected EditUserDTO saveUser(EditUserDTO userDto) throws FieldValidationException {
		Class<?> validationGroup;
		if (userDto.getId() == null) {
			if (getCurrentUser().isSaferAdmin()) {
				validationGroup = SaferAdminCreateUser.class;
			} else {
				validationGroup = AdminCreateUser.class;
			}
		} else if (userDto.getId().equals(getCurrentUser().getId())) {
			validationGroup = EditMyProfile.class;
		} else {
			validationGroup = AdminEditOtherUser.class;
		}
		if (!validator.validate(userDto, validationGroup).isEmpty()) {
			throw new FieldValidationException("Invalid data");
		}

		User user;
		userDto.setEnabled(userDto.getEnabled()==null?false:userDto.getEnabled());
		
		if (userDto.getId() != null) {
			user = userManager.getUser(userDto.getId());
			if (user.getId().equals(saferContext.getImpersonatingUserId())){
				throw new FieldValidationException(Labels.EDIT_OWN_USER_WHILE_IMPERSONATING_DENIED);
			}
			
			boolean roleDownGrade = Role.getAllRoles(userDto.getRole()).size()<Role.getAllRoles(user.getRole()).size();
			
			if( 
					( 
							( !userDto.getEnabled() && !userDto.getEnabled().equals(user.getEnabled()) )  
							|| roleDownGrade 
					) 
					&& Role.getLowerRoles(getCurrentUser().getRole()).indexOf(user.getRole())==-1 
					&& !getCurrentUser().isSuperAdmin()){
				throw new FieldValidationException("You cannot disable or downgrade this SAFER One user with your current permissions");
			}
			
			if (!getCurrentUser().isSiteAdmin()) {
				if (getCurrentUser().getId().equals(user.getId())) {
					// only admins are allowed to activate users and we ignore
					// the value from my account so we don't lock out by
					// accident
					userDto.setEnabled(user.getEnabled());
				}
				// non admins can't set some data
				userDto.setSites(mapper.mapAsList(user.getSites(), SiteDTO.class));
				userDto.setOrganization(mapper.map(user.getOrganization(), OrganizationDTO.class));
				userDto.setRole(user.getRole());
			}
			if (getCurrentUser().getId().equals(user.getId())) {
				SiteDTO currentSiteDTO = new SiteDTO();
				currentSiteDTO.setId(getCurrentSite().getId());
				if (userDto.getSites().indexOf(currentSiteDTO) == -1){
					throw new FieldValidationException("Current site has to be selected");
				}
			}
			// email can't be changed
			userDto.setEmail(user.getEmail());
			mapper.map(userDto, user);
		} else {
			User existing = userManager.getUser(userDto.getEmail(),true);
			user = mapper.map(userDto, User.class);
			if ( existing != null) {
				if(existing.isSmr()) {
					user.setId(existing.getId());
					user.setSmrUserId(existing.getSmrUserId());
					user.setSmr(false);
				}else {
					if(getCurrentUser().isSaferAdmin()){
						throw new FieldValidationException(FieldVerifier.INVALID_EMAIL_TAKEN);
					}else{
						throw new FieldValidationException(FieldVerifier.INVALID_EMAIL_TAKEN);
					}
				}
			}
			if (!getCurrentUser().isSiteAdmin()) {
				// all new user, not created by site admin, have to be enabled
				// by site admin
				user.setEnabled(false);
			}
			String salt = UserManagerImpl.generateSalt(userDto.getEmail());
			user.setIedAccess(true);
			user.setSalt(salt);
			user.setPassword(UserManagerImpl.encryptPassword(salt, userDto.getNewPassword()));
			user.setCreatedDate(new Date());
			user.setLastPasswordChange(new Date());
			if (!getCurrentUser().isSaferAdmin()) {
				user.setOrganization(getCurrentUser().getOrganization());
			}
		}
		if(user.getLastSite()!=null && !userManager.isAssignedToSite(user, user.getLastSite())) {
			user.setLastSite(user.getSites().get(0));
		}
		user = userManager.save(user);
		user.setModifiedBy(getCurrentUser().getId());
		SaferContext.updateUser(user);
		return mapper.map(user, EditUserDTO.class);
	}

	@SuppressWarnings("unchecked")
	private <T extends HasId> T mapOrNewInstance(Object o, Class<?> clazz) {
		if (o == null) {
			try {
				return (T) clazz.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				return null;
			}
		}
		return (T) mapper.map(o, clazz);
	}

	@Override
	public void changePassword(int userId, String oldPassword, String newPassword) throws FieldValidationException {
		newPassword = HtmlUtils.escapeHtml(newPassword);
		
		User user = getCurrentUser();
		User userToBeUpdated = userManager.getUser(userId);
		List<LastPassword> lastPasswords=userToBeUpdated.getLastPasswords();
		if(lastPasswords==null){
			lastPasswords=new ArrayList<LastPassword>();
		}
		
		if (! (user.equals(userToBeUpdated) || user.isSuperAdmin())
				/*&& !(user.isOrganizationAdmin() && user.getOrganization().equals(userToBeUpdated.getOrganization()))
				&& !(user.isSiteAdmin() && user.getSites().containsAll(userToBeUpdated.getSites()))*/
				) {
			throw new FieldValidationException("You do not have the right to update password for this user !");
		}

		if (user.equals(userToBeUpdated)) {
			String hashFromDB = userToBeUpdated.getPassword();
			if (!hashFromDB.equals(UserManagerImpl.encryptPassword(user.getSalt(), oldPassword))) {
				throw new FieldValidationException(FieldVerifier.INVALID_INCORRECT_OLD_PASSWORD);
			}
		}
		//||userToBeUpdated.getId().toString().equals(newPassword)
		if (newPassword.toLowerCase().indexOf(userToBeUpdated.getEmail().toLowerCase())>=0||userToBeUpdated.getEmail().toLowerCase().equals(newPassword.toLowerCase())) {
				throw new FieldValidationException(FieldVerifier.INVALID_INCORRECT_ID_PASSWORD);
		}
		/*
		if(userToBeUpdated.getSmrUserId()!=null){
			
			if (userToBeUpdated.getId().toString().equals(newPassword)) {
				throw new FieldValidationException(FieldVerifier.INVALID_INCORRECT_ID_PASSWORD);
		}			
		}
        */
		if (userToBeUpdated.getPassword().equals(UserManagerImpl.encryptPassword(userToBeUpdated.getSalt(), newPassword))){
			
			throw new FieldValidationException(FieldVerifier.INVALID_NO_OLD_PASSWORD);
		}

		Integer resetHours=0;
		Organization organization=user.getOrganization();
		if(organization==null){
			
		}else{
			resetHours=organization.getResetPasswordHour();
			if(resetHours==null)resetHours=0;
		}		
		//Organization organization=userToBeUpdated.getOrganization();
		
		
		//Integer resetHours=organization.getResetPasswordHour();
		//if(resetHours==null)resetHours=0;
		
		if(resetHours>0&&userToBeUpdated.getLastPasswordChange()!=null)
		if(((new Date()).getTime()-userToBeUpdated.getLastPasswordChange().getTime())< resetHours * 60 * 60 * 1000L &&userToBeUpdated.getLastPasswords()!=null){
			
			throw new FieldValidationException("You are allowed to change password once every "+resetHours.toString()+" hours");
		}		
		
		
		
		
		Integer numberOfPassword=organization.getNumberOfPasswords();
		if(numberOfPassword==null)numberOfPassword=1;
		
		Integer bIndex=lastPasswords.size()>numberOfPassword?lastPasswords.size()-numberOfPassword:0;
		
		for(int i=bIndex;i<lastPasswords.size();i++){
			
			
			if (lastPasswords.get(i).getPassword().equals(UserManagerImpl.encryptPassword(lastPasswords.get(i).getSalt(), newPassword))){
				
				throw new FieldValidationException(FieldVerifier.INVALID_NO_OLD_PASSWORD);
			}			
			
			
		}
		
		
		String salt = UserManagerImpl.generateSalt(user.getEmail());
		String cryptedPassword = UserManagerImpl.encryptPassword(salt, newPassword);
		
		userToBeUpdated.setPassword(cryptedPassword);
		userToBeUpdated.setSalt(salt);
		userToBeUpdated.setLastPasswordChange(new Date());
		userToBeUpdated.setModifiedBy(getCurrentUser().getId());
		
		for(int i=0;i<bIndex;i++){
			lastPasswords.remove(0);
			
		}
		if(lastPasswords.size()==numberOfPassword){
			lastPasswords.remove(0);
		}
		lastPasswords.add(new LastPassword(cryptedPassword,salt));
		
		userToBeUpdated.setLastPasswords(lastPasswords);
		userManager.save(userToBeUpdated);

		if (user.equals(userToBeUpdated)) {
			user.setSalt(salt);
			user.setPassword(cryptedPassword);
		}
	}

	@Override
	public void deleteRecord(int[] ids, RecordType type) throws FieldValidationException {
		switch (type) {
		case USER:
			deleteUser(ids);
			break;
		case ORGANIZATION:
			deleteOrganization(ids);
			break;
		case MET_STATION:
			deleteMetStation(ids);
			break;
		case POI:
			deletePoi(ids);
			break;
		case EMISSION_SOURCE:
			deleteEmissionSource(ids);
			break;
		case SITE:
			deleteSite(ids);
			break;
		case CASCADE_SITE:
			cascadeDeleteSite(ids);
			break;
		case DAQ:
			deleteDaq(ids);
			break;
		case PORT:
			deletePort(ids);
			break;
		case EVENT:
			deleteEvent(ids);
			break;
		case KML_LAYER:
			deleteKmlLayer(ids);
			break;
		case CHEMICAL:
			deleteChemical(ids);
			break;
		case SENSOR:
			deleteSensor(ids);
			break;
		case SENSOR_RAE:
			deleteSensorRae(ids);
			break;
		case SENSOR_GROUP:
			deleteSensorGroup(ids);
			break;
		case SCENARIO_RUN:
		//case SCENARIO_HISTORY:
			deleteScenarioRun(ids);
			break;
		case SCENARIO:
			deleteScenario(ids);
			break;
		case SENSOR_INTERFACE:
			deleteSensorInterface(ids);
			break;
		case TIPOFTHEDAY:
			deleteTipOfTheDay(ids);
			break;
		case FEEDBACK:
			deleteFeedback(ids);
			break;
		default:
			throw new RuntimeException("Please update deleteRecord method with type  " + type.toString());
		}
	}

	private void cascadeDeleteSite(int[] ids) throws FieldValidationException {
		if (!Boolean.TRUE.equals(getCurrentUser().isSuperAdmin())){
			throw new FieldValidationException("You are not allowed to cascade delete sites");
		}
		
		for (int siteId:ids) {
			Site site = siteManager.getSite(siteId);
			List<EmissionSource> es = emissionManager.getEmissionRepository().findBySiteAndHiddenFalseAndLocationNotNull(site);
			for (EmissionSource i:es) {
				i.setHidden(true);
				emissionManager.save(i);
			}
			
			List<MetStation> metStations = dataSourceManager.getMetStations(site);
			for (MetStation met:metStations) {
				met.setHidden(true);
				dataSourceManager.save(met);
			}
			
			List<PointOfInterest> pois = poisManager.getPoiRepository().findBySiteAndHiddenFalse(site);
			for (PointOfInterest poi:pois) {
				poi.setHidden(true);
				poisManager.save(poi);
			}
			
			List<Scenario> scenarios = scenarioManager.getScenarios(site);
			for (Scenario record:scenarios) {
				record.setHidden(true);
				scenarioManager.saveScenario(record);
			}
			
			List<Sensor> sensors = dataSourceManager.getSensors(site);
			for (Sensor record:sensors) {
				record.setHidden(true);
				dataSourceManager.saveSensor(record);
			}
			
			List<SensorInterface> interfaces = dataSourceManager.getSensorInterfaceBySite(site);
			for (SensorInterface record:interfaces) {
				record.setHidden(true);
				dataSourceManager.saveSensorInterface(record);
			}
			
			List<SensorRae> raes = dataSourceManager.getSensorsRae(site);
			for (SensorRae record:raes) {
				record.setHidden(true);
				dataSourceManager.saveSensorRae(record);
			}
			
			List<Zone> zones = zoneManager.getZonesBySite(site);
			for (Zone record:zones) {
				record.setHidden(true);
				zoneManager.save(record);
			}
			
			List<Daq> daqs = daqManager.getDaqs(site);
			for (Daq record:daqs) {
				record.getSites().remove(site);
				if (record.getSites().size() == 0) {
					record.setHidden(true);
				}
				daqManager.save(record);
			}
			
			List<User> users = userManager.getUsers(site);
			for (User record:users) {
				record.getSites().remove(site);
				if (record.getSites().size() == 0) {
					record.setHidden(true);
				}
				userManager.save(record);
			}
			
			site.setHidden(true);
			siteManager.save(site);
		}
		
	}

	private void deleteChemical(int[] ids) {
		chemicalsManager.deleteChemical(ids);
	}

	@Transactional
	private void deleteEvent(int[] historyIds) throws FieldValidationException {
		List<ErgEvent> toDelete = new ArrayList<ErgEvent>();
		List<ScenarioRun> toDeleteScen = new ArrayList<ScenarioRun>();
		List<EventHistory> toDeleteHist = new ArrayList<EventHistory>();
		boolean exception = false;
		for (int id : historyIds) {
		    EventHistory evHist = eventHistoryManager.getEventHistoryReposittory().findOne(id);
		    if(exception=(evHist.getUserId()!=getCurrentUser().getId()))
			break;
		    if(evHist!=null){
			toDeleteHist.add(evHist);
			Integer eventId = evHist.getEventId();
			if(eventId!=null&&eventId>=0){
				if(evHist.isErg() ){
					ErgEvent event = eventManager.getErgEvent(eventId);
					if (event.getUser() != null && event.getUser().equals(getCurrentUser())) {
						toDelete.add(event);
					} else {
						exception = true; 
						break;
					}
				}else{
					ScenarioRun scenRun = scenarioManager.getScenarioRun(eventId);
					if (scenRun.getUser() != null && scenRun.getUser().equals(getCurrentUser())) {
						toDeleteScen.add(scenRun);
					} else {
						exception = true; 
						break;
					}
				}
			}
		    }
		}
		
		if(exception){
		    throw new FieldValidationException("You can delete only your own events");
		}
		for (ErgEvent event: toDelete) {
		    event.setHidden(true);
		    eventManager.saveErgEvent(event);
		}
		for (ScenarioRun scen: toDeleteScen) {
		    scen.setHidden(true);
		    scenarioManager.saveScenarioRun(scen);
		}
		for (EventHistory evHist: toDeleteHist) {
		    evHist.setHidden(true);
		    eventHistoryManager.save(evHist);
		}
	}
	
	//TODO Cristi - we need to do this
//	private boolean checkIfUserCanDeleteEvent(User user, User eventUser, Site eventSite) {
//		if(eventUser != null && eventUser.equals(getCurrentUser())){
//			return true;
//		}
//	    if(user.isOrganizationAdmin() && user.getOrganization().equals(eventUser.getOrganization())) {
//	    	return true;
//	    }
//		if(user.isSiteAdmin() && user.getSites().contains(eventSite)) {
//			return true;
//		}
//		return false;
//	}

	private void deleteScenarioRun(int[] ids) throws FieldValidationException {
		List<ScenarioRun> toDelete = new ArrayList<ScenarioRun>();
		for (int id : ids) {
			ScenarioRun event = scenarioManager.getScenarioRun(id);
			toDelete.add(event);
		}
		for (ScenarioRun event: toDelete) {
			event.setHidden(true);
			scenarioManager.saveScenarioRun(event);
		}
	}
	
	private void deleteScenario(int[] ids) throws FieldValidationException {
		List<Scenario> toDelete = new ArrayList<Scenario>();
		for (int id : ids) {
			Scenario event = scenarioManager.getScenario(id);
			toDelete.add(event);
		}
		for (Scenario s: toDelete) {
			s.setHidden(true);
			if (s.getEmissionSource().isScenarioHelper()) {
				s.getEmissionSource().setHidden(true);
				scenarioManager.saveScenario(s, true);
			} else {
				scenarioManager.saveScenario(s, false);
			}
		}
	}
	
	private void deleteEmissionSource(int[] ids) {
		for (int id : ids) {
			EmissionSource em = emissionManager.getEmissionSource(id);
			em.setHidden(true);
			emissionManager.save(em);
		}
	}

	private void deletePoi(int[] ids) {
		for (int id : ids) {
			PointOfInterest poi = poisManager.getPoiById(id);
			poi.setHidden(true);
			poisManager.save(poi);
		}
	}
	
	private void deleteSensor(int[] ids) {
		for (int id : ids) {
			Sensor sensor = dataSourceManager.getSensor(id);
			deleteSensor(sensor);
		}
	}
	
	private void deleteSensor(Sensor sensor) {
		sensor.setHidden(true);
		dataSourceManager.saveSensor(sensor);
		alarmManager.deleteSensorAlarms(sensor);
}
	
	private void deleteSensorRae(int[] ids) {
		for (int id : ids) {
			SensorRae sensor = dataSourceManager.getSensorRaeById(id);
			deleteSensorRae(sensor);
		}
	}
	
	private void deleteSensorRae(SensorRae rae) {
		for(Sensor sen : dataSourceManager.getSensorsByRae(rae)) {
			deleteSensor(sen);
		}
		rae.setHidden(true);
		dataSourceManager.saveSensorRae(rae);
		alarmManager.deleteSensorRaeAlarms(rae);
	}
	
	private void deleteSensorGroup(int[] ids) {
		for (int id : ids) {
			SensorGroup sensor = dataSourceManager.getSensorGroupById(id);
			deleteSensorGroup(sensor);
		}
	}
	
	private void deleteSensorGroup(SensorGroup group) {
		for(Sensor sen : dataSourceManager.getSensorsByGroup(group)) {
			deleteSensor(sen);
		}
		group.setHidden(true);
		dataSourceManager.saveSensorGroup(group);
	}
	
	private void deleteKmlLayer(int[] ids) {
		for (int id : ids) {
			KmlLayer kml = kmlLayerManager.getKmlLayer(id);
			kml.setHidden(true);
			kmlLayerManager.saveKmlLayer(kml);
			//TODO: should we delete the file on the disk ?
		}
	}

	private void deleteMetStation(int[] ids) throws FieldValidationException {
		if (!getCurrentUser().isSiteAdmin()) {
			throw new FieldValidationException("You don't have the right to delete record");
		}
		for (int id : ids) {
			MetStation met = dataSourceManager.getMetStation(id);
			met.setHidden(true);
			dataSourceManager.save(met);
			alarmManager.deleteMetAlarms(met);
		}
	}

	private void deleteDaq(int[] ids) {
		for (int id : ids) {
			Daq daq = daqManager.getDaq(id);
			daq.setHidden(true);

			List<MetStation> allMets = dataSourceManager.getMetStationsForDaq(daq);
			for (MetStation met : allMets) {
				met.setDaq(null);
				met.setPort(null);
				dataSourceManager.save(met);
				alarmManager.closeMetAlarms(met);
			}
			
			alarmManager.closeSensorAlarms(daq);
			alarmManager.closeSensorRaeAlarms(daq);
			List<SensorInterface> allSensorInterfaces = dataSourceManager.getSensorInterfacesForDaq(daq);
			for (SensorInterface si : allSensorInterfaces) {
				si.setDaq(null);
				si.setPort(null);
				dataSourceManager.saveSensorInterface(si);
			}
			
			daqManager.save(daq);
			for (Site site : siteManager.getSites(daq.getOrganization().getId(),false)) {
				alarmManager.refreshDaqOfflineAlarm(daq, site);
			}
		}
	}

	private void deletePort(int[] ids) {
		for (int id : ids) {
			Port port = daqManager.getPort(id);
			port.setHidden(true);
			daqManager.save(port);
		}
	}
	
	private void deleteTipOfTheDay(int[] ids){
		if(!getCurrentUser().isSaferAdmin()){
			throw new RuntimeException("Not Allowed");
		}
		for (int id : ids) {
			TipOfTheDay tip = tipOfTheDayManager.getTipOfTheDay(id);
			tip.setHidden(true);
			tipOfTheDayManager.save(tip);
		}
	}
	
	private void deleteFeedback(int[] ids) {
		if(!getCurrentUser().isSaferAdmin()){
			throw new RuntimeException("Not Allowed");
		}
		for (int id : ids) {
			Feedback feedback = feedbackManager.getFeedback(id);
			feedback.setHidden(true);
			feedbackManager.save(feedback);
		}
	}

	private void deleteSite(int[] ids) {
		for (int id : ids) {
			Site site = siteManager.getSite(id);
			site.setHidden(true);

			// TODO -low- increase performance for below test
			List<User> allUsers = userManager.getUserRepository().findByOrganizationAndHiddenFalseAndSmrFalse(site.getOrganization());
			for (User u : allUsers) {
				if (u.getSites().contains(site)) {
					throw new RuntimeException("Please first delete all users for " + site.getName() + " site");
				}
			}

			siteManager.save(site);
		}
		// TODO: refresh the currentuser
	}

	@Secured("ROLE_SITE_ADMIN")
	private void deleteSensorInterface(int[]ids) throws FieldValidationException{
		if (!getCurrentUser().isSiteAdmin()) {
			throw new FieldValidationException("You are not allowed to delete sensor interfaces");
		}
		List<SensorInterface> toDelete = new ArrayList<SensorInterface>();
		for (int id : ids) {
			SensorInterface si = dataSourceManager.getSensorInterfaceById(id);
			if(!SensorInterfaceType.RAE.equals(si.getType()) && dataSourceManager.getSensorInterfaceSensorCount(si) > 0) {
				throw new RuntimeException("Please first delete all sensors for interface " + si.getName() );
			}
			toDelete.add(si);
		}
		
		for (SensorInterface si:toDelete) {
			si.setHidden(true);
			dataSourceManager.saveSensorInterface(si);

			if(SensorInterfaceType.RAE.equals(si.getType())) {
				List<SensorRae> raes = dataSourceManager.getSensorRaes(si);
				for(SensorRae rae : raes) {
					for(Sensor sen : dataSourceManager.getSensorsByRae(rae)) {
						deleteSensor(sen);
					}
					rae.setHidden(true);
					dataSourceManager.saveSensorRae(rae);
					alarmManager.deleteSensorRaeAlarms(rae);
				}
			}
			
		}
	}
	
	// TODO - low priority - improve performance to generate one sql for all
	// organization deletions
	private void deleteOrganization(int[] ids) throws FieldValidationException {
		User currentUser = getCurrentUser();
		if (!currentUser.isSaferAdmin()) {
			throw new FieldValidationException("You are not allowed to delete organizations");
		}
		List<Organization> toDelete = new ArrayList<Organization>();

		for (int id : ids) {
			Organization org = organizationManager.getOrganization(id);
			Integer impersonatingUserOrganizationId = null;
			if(saferContext.getImpersonatingUserId() != null) {
				impersonatingUserOrganizationId = userManager.getUser(saferContext.getImpersonatingUserId()).getOrganization().getId();
			}
			if (!currentUser.isSaferAdmin() || currentUser.getOrganization().getId().equals(id) || Integer.valueOf(id).equals(impersonatingUserOrganizationId)) {
				throw new FieldValidationException("You are not allowed to delete this organization");
			}
			List<Site> allSites = siteManager.getSites(id,false);
			if (allSites != null && allSites.size() > 0) {
				throw new FieldValidationException("Please first delete all sites for this organization");
			}
			List<User> allUsers = userManager.getUserRepository().findByOrganizationAndHiddenFalseAndSmrFalse(new Organization(id));
			if (allUsers != null && allUsers.size() > 0) {
				throw new FieldValidationException("Please first delete all users for this organization");
			}
			toDelete.add(org);
		}

		for (Organization org : toDelete) {
			org.setHidden(true);
			organizationManager.save(org);
		}
	}

	// TODO - low priority - improve performance to generate one sql for all
	// user deletions
	private void deleteUser(int[] ids) throws FieldValidationException {

		User currentUser = getCurrentUser();
		List<User> toDelete = new ArrayList<User>();
		for (Integer i : ids) {
			User u = userManager.getUser(i);
			if (  i.equals(currentUser.getId()) 
					|| i.equals(saferContext.getImpersonatingUserId()) 
					|| !currentUser.isSiteAdmin()
					|| !currentUser.isSaferAdmin() && !currentUser.getOrganization().equals(u.getOrganization())
					|| !currentUser.isOrganizationAdmin() && !currentUser.hasAnySite(u.getSites()) 
					|| Role.getLowerRoles(getCurrentUser().getRole()).indexOf(u.getRole())==-1 && !currentUser.isSuperAdmin()
					) {
				throw new FieldValidationException("You are not allowed to delete this user");
			}
			toDelete.add(u);
		}

		for (User u : toDelete) {
			u.setHidden(true);
			u.setModifiedBy(getCurrentUser().getId());
			userManager.save(u);
		}
	}

	@Override
	public void setPrimaryMetStation(int id) {
		User currentUser = getCurrentUser();
		Site currentSite = getCurrentUser().getCurrentSite();

		dataSourceManager.setPrimaryMetStation(currentUser, currentSite, id);
	}

	@Override
	public Units getUnits() {
		return unitsManager.getUnits(getCurrentSite().getId(),getCurrentUser().getId());
	}

	@SuppressWarnings("unchecked")
	@Override
	public Units saveUnits(List<UnitSettingWithGroupDTO> settings) {
		settings = (List<UnitSettingWithGroupDTO> ) HtmlUtils.escapeHtml(settings);
		unitsManager.save(settings, getCurrentSite().getId(),getCurrentUser().getId());
		return unitsManager.getUnits(getCurrentSite().getId(),getCurrentUser().getId());
	}

	public void TestConversion() {
		TestUnitDTO test = new TestUnitDTO();
		test.setLength(33);
		unitsManager.toInternal(test, getCurrentSite().getId(),getCurrentUser().getId());
		System.out.println(test);
		unitsManager.toCustom(test, getCurrentSite().getId(),getCurrentUser().getId());
		System.out.println(test);
	}

	@Override
	public ListLoadResult<EarthNetworkStationDTO> getApiStationsList(PointDTO location, String username, String password)
			throws InvalidEarthNetworkCredentialsException, EarthNetworkApiUnreachableException {

		username = getEarthNetworkKey();
		Site currentSite = saferContext.getCurrentSite();
		PointDTO refPoint = new PointDTO(location.getLatitude(), location.getLongitude());
		List<EarthNetworkStationDTO> stations = earthNetworkApiClient.getStations(username, location, refPoint);
		Iterator<EarthNetworkStationDTO> it = stations.iterator();
		while(it.hasNext()) {
			EarthNetworkStationDTO station = it.next();
			if(station.getDistance()>currentSite.getRadius()) {
				it.remove();
			}
		}
		Collections.sort(stations, new Comparator<EarthNetworkStationDTO>() {

			@Override
			public int compare(EarthNetworkStationDTO o1, EarthNetworkStationDTO o2) {
				return o1.getDistance().compareTo(o2.getDistance());
			}
		});
		ListLoadResult<EarthNetworkStationDTO> result = new ListLoadResultBean<EarthNetworkStationDTO>(stations);
		return result;
	}

	@Override
	public boolean validateEarthNetworkApiCredentials(String username, String password) throws EarthNetworkApiUnreachableException {
		username = getEarthNetworkKey();
		if (StringUtils.isEmpty(username)) {
			return false;
		}
		return earthNetworkApiClient.auth(username);
	}

	@Override
	public List<String> getTimeZones() {
		return Arrays.asList(TimeZone.getAvailableIDs());
	}

	@Override
	public String getTimeForLocation(PointDTO location) {
		GeoApiContext context = new GeoApiContext().setApiKey("AIzaSyCzmBr4pyiaRuG_5UkXdJtp7lQGmUIwY40");
		PendingResult<TimeZone> res = com.google.maps.TimeZoneApi.getTimeZone(context,
				new LatLng(location.getLatitude(), location.getLongitude()));
		TimeZone t = null;
		try {
			t = res.await();
		} catch (Exception e) {
			throw new RuntimeException("Excepion getting timezone. Message:" + e.getMessage());
		}
		return t.getID();
	}

	@Override
	public List<SiteDTO> getAllSites(int organizationId) {
		// TODO: validate that a user cannot view sites from other organizations
		List<Site> sites;
		if (getCurrentUser().isOrganizationAdmin()) {
			sites = siteManager.getSites(organizationId,true);
		} else {
			sites = siteManager.getSitesByUser(getCurrentUser());
		}
		return mapper.mapAsList(sites, SiteDTO.class);
	}

	@Override
	public List<MetStationDTO> getAllMetStations(List<Integer> siteIds) {
		// TODO: validate that a user cannot view met stations from other
		// organizations
		List<MetStation> mets = dataSourceManager.getMetStationsFromSites(siteIds);
		return mapper.mapAsList(mets, MetStationDTO.class);
	}
	
	@Override
	public List<MetStationDTO> getAllMetStations(int[] daqIds) {
		// TODO: validate that a user cannot view met stations from other
		// organizations
		List<MetStation> mets = new ArrayList<MetStation>();
		for(int id : daqIds) {
			Daq daq = new Daq();
			daq.setId(id);
			mets.addAll(dataSourceManager.getMetStationsForDaq(daq));
		}
		return mapper.mapAsList(mets, MetStationDTO.class);
	}
	
	@Override
	public List<MetStationDTO> getMetStationsForCurrentSite(int daqId) {
		// TODO: validate that a user cannot view met stations from other
		// organizations/sites
		List<MetStation> mets = new ArrayList<MetStation>();
		Daq daq = new Daq();
		daq.setId(daqId);
		Site currentSite = getCurrentUser().getCurrentSite();
		mets.addAll(dataSourceManager.getMetStationsForDaqAndSite(daq,currentSite));
		
		return mapper.mapAsList(mets, MetStationDTO.class);
	}
	
	@Override
	public List<RainDataDTO> getRainData(int metStationId,Date selectedDate) {
		
		MetStation met = dataSourceManager.getMetStation(metStationId);
		
		Site currentSite = getCurrentUser().getCurrentSite();
		String timezone = currentSite.getTimeZone();
		//Date dayStart = DateUtils.trimToDay(selectedDate);
		Date endTime = DateUtils.add(DateUtils.trimToHour(selectedDate),Calendar.SECOND,-1);
		Date aDayAgo = DateUtils.add(endTime,Calendar.DAY_OF_YEAR,-1);
		Date start = DateUtils.convertFromTimezoneToUtc(aDayAgo, timezone);
		Date end = DateUtils.convertFromTimezoneToUtc(endTime, timezone);
		Map<Date, Double> rainfallMap = new LinkedHashMap<>();
		List<MetAverage> averages = dataSourceManager.getMetAverages(met, MetAverageType.FIVE_MINUTE_AVERAGE, start, end);
		for(MetAverage avg : averages) {
			if(avg.getMetData().getRainRate()!=null) {
				Date dateTaken =  DateUtils.convertToTimezone(avg.getDateTaken(), timezone);
				Date hourDate = DateUtils.trimToHour(dateTaken);
				if(!rainfallMap.containsKey(hourDate)) {
					rainfallMap.put(hourDate, 0d);
				}
				rainfallMap.put(hourDate, rainfallMap.get(hourDate) + avg.getMetData().getRainRate());
			}
		}
		List<RainDataDTO> rainList = new ArrayList<RainDataDTO>();
		for(Entry<Date,Double> entry : rainfallMap.entrySet()) {
			Double rainCustom = getUnits().toCustomAllowNull(UnitParam.IDP_MET_RAINFALL, entry.getValue());
			RainDataDTO rain = new RainDataDTO();
			rain.setDate(entry.getKey());
			rain.setRainRate(rainCustom);
			rainList.add(rain);
		}
		return rainList;
	}

	@Override
	public ManualMetDataDTO getManualMetData() {
		User currentUser = getCurrentUser();
		Site currentSite = currentUser.getCurrentSite();
		ManualMetData manualMet = dataSourceManager.getManualMetData(currentUser, currentSite);
		if(manualMet == null) {
			manualMet = new ManualMetData();
			MetData manualMetData = new MetData();
			manualMetData.sethStability(3);
			manualMetData.setvStability(3);
			manualMet.setMetData(manualMetData);
		}
		ManualMetDataDTO result =  mapOrNewInstance(manualMet, ManualMetDataDTO.class);
		unitsManager.toCustom(result.getMetData(), getCurrentSite().getId(),getCurrentUser().getId());
		return result;
	}
	
	@Override
	public HashMap<Integer, PointOfInterestDTO> getGooglePlaces(Integer groupId){
		final boolean forEvent = (groupId!=null);
		List<PointOfInterestDTO> pois = mapper.mapAsList(
				poisManager.getPoiRepository().findBySiteAndHiddenFalseAndGroupId(getCurrentSite(),groupId),
				PointOfInterestDTO.class);
		HashMap<Integer, PointOfInterestDTO> m = new HashMap<Integer, PointOfInterestDTO>();
		Set <String> sourceSet = new HashSet<String>();
		for (PointOfInterestDTO p : pois) {
			m.put(p.getId(), p);
			sourceSet.add(p.getSourceId());
		}
		
		if(forEvent){
			List<PointOfInterestDTO> existingPois = mapper.mapAsList(
					poisManager.getPoiRepository().findBySiteAndHiddenFalseAndGroupId(getCurrentSite(),null),
					PointOfInterestDTO.class);
			for (PointOfInterestDTO p : existingPois) {
				if(StringUtils.isNotBlank(p.getSourceId()) && !sourceSet.contains(p.getSourceId()) ){
					m.put(p.getId(), p);
				}
			}
		}
		
		return m;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public HashMap<Integer, PointOfInterestDTO> importGooglePlacesResults(ArrayList<PointOfInterestDTO> selected) {
		selected = (ArrayList<PointOfInterestDTO>) HtmlUtils.escapeHtml(selected);
		
		List<PointOfInterest> pois = mapper.mapAsList(selected, PointOfInterest.class);
		for (PointOfInterest poi : pois) {
			poi.setSite(saferContext.getCurrentUser().getCurrentSite());
			poi.setCreatedDate(new Date());
			poi.setEnabled(true);
		}
		List<PointOfInterest> saved = poisManager.saveButSkipExisting(pois);

		List<PointOfInterestDTO> savedDTO = mapper.mapAsList(saved, PointOfInterestDTO.class);
		HashMap<Integer, PointOfInterestDTO> hash = new HashMap<Integer, PointOfInterestDTO>();

		for (PointOfInterestDTO p : savedDTO) {
			hash.put(p.getId(), p);
		}

		return hash;
	}

	@Override
	public String impersonate(int userId) throws RuntimeException {
		User currentUser = getCurrentUser();
		if (userId == currentUser.getId()) {
			throw new RuntimeException("You can't impersonate yourself");
		}

		if (!currentUser.isSaferAdmin()) {
			throw new RuntimeException("You are not allowed to impersonate users");
		}
		
		if (saferContext.getImpersonatingUserId() != null) {
			throw new RuntimeException("Impersonating users can't impersonate other users");
		}
		
		User toImpersonate = userManager.getUser(userId);
		
		if(toImpersonate==null){
			throw new RuntimeException("Error impersonating. Please contact support.");
		}
		
		if (!currentUser.isSuperAdmin()) {
			emailManager.sendWarningImpersonate(currentUser.getEmail(), toImpersonate.getEmail());
		}
		
		if(toImpersonate.isSuperAdmin() && !currentUser.isSuperAdmin()){
			throw new RuntimeException("You are not allowed to impersonate this user");
		}
		
		logOut(false);
		
		getThreadLocalRequest().getSession().invalidate();
		getThreadLocalRequest().getSession(true);
		saferContext = ctx.getBean(SaferContext.class);
		saferContext.setCurrentUser(currentUser);
		saferContext.impersonate(toImpersonate);
		return getThreadLocalRequest().getSession(true).getId();
	}

	@Override
	public String resetImpersonation() throws RuntimeException {
		if (saferContext.getImpersonatingUserId() == null) {
			throw new RuntimeException("You are not impersonating users");
		}
		User currentUser = userManager.getUser(saferContext.getImpersonatingUserId());
		logOut(false);
		getThreadLocalRequest().getSession(true);
		saferContext = ctx.getBean(SaferContext.class);
		saferContext.setCurrentUser(currentUser);
		return getThreadLocalRequest().getSession(true).getId();
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public PagingLoadResult<PortDTO> getPorts(PagingLoadConfig config, DaqDTO daqDTO) {
		Page page = null;
		List dto = null;
		PageRequest pageable;

		if (config.getSortInfo().size() == 1) {
			SortInfo sortInfo = config.getSortInfo().get(0);
			pageable = new PageRequest(config.getOffset() / config.getLimit(), config.getLimit(), gxtToJpaSort.get(sortInfo.getSortDir()),
					sortInfo.getSortField());
		} else {
			pageable = new PageRequest(config.getOffset() / config.getLimit(), config.getLimit());
		}

		Daq daq = mapper.map(daqDTO, Daq.class);
		page = daqManager.getPorts(daq, pageable);
		dto = mapper.mapAsList(page.getContent(), PortDTO.class);

		PagingLoadResult result = new PagingLoadResultBean<UserDTO>(dto, (int) page.getTotalElements(), config.getOffset());
		return result;
	}

	@Override
	public List<DaqDTO> getDaqsForCurrentSite() {
		List<Daq> daqs = 	daqManager.getDaqs(getCurrentUser().getCurrentSite());
		List<DaqDTO> result = mapper.mapAsList(daqs, DaqDTO.class);
		return result;
	}
	
	@Override
	public List<SensorDTO> getSensorsForRae(Integer raeId) {
		SensorRae rae = new SensorRae();
		rae.setId(raeId);
		return mapper.mapAsList(dataSourceManager.getSensorsByRae(rae), SensorDTO.class);
	}

	@Override
	public String generateDaqKey() {
		String daqKey;
		do {
			daqKey = DigestUtils.sha1Hex(Calendar.getInstance().getTime().toString().getBytes());
		} while (!daqManager.getDaqsByKey(daqKey,false).isEmpty());
		return daqKey;
	}

	@Override
	public ErgServerData getErgData(Date releaseTime, PointDTO location, Integer metStationId){
		ErgServerData data = new ErgServerData();
		Site currentSite = getCurrentUser().getCurrentSite();
		MetStation met;
		if (metStationId != null) {
			met = dataSourceManager.getMetStation(metStationId);
		} else {
			met = null;
		}
		
		if (releaseTime == null) {
			if (met != null) {
				MetAverage latestAvg = dataSourceManager.getLastMetAverageOrInstant(met, MetAverageType.ONE_MINUTE_AVERAGE);
				if (latestAvg != null) {
					releaseTime = latestAvg.getDateTaken();
				}
			}
		} else {
			releaseTime = DateUtils.convertFromTimezoneToUtc(releaseTime, currentSite.getTimeZone());
		}
		
		if (releaseTime != null && met != null) {
			SiteSettings siteSettings = siteManager.getSiteSettings(getCurrentSite());
			Date end = new Date(releaseTime.getTime()+(siteSettings.getEventAvgInterval()-1)*60*1000);
			List<MetAverage> metAverages = dataSourceManager.getMetAverages(met, MetAverageType.ONE_MINUTE_AVERAGE, new Date(releaseTime.getTime() - 5*60*1000), end);
			if(metAverages.isEmpty()) {
				metAverages = dataSourceManager.getMetAverages(met, MetAverageType.INSTANT, new Date(releaseTime.getTime() - 5*60*1000), end);
			}
			MetAverage latest = dataSourceManager.getLastMetAverageOrInstant(met, MetAverageType.ONE_MINUTE_AVERAGE);
			PointDTO metLocation = null;
			
			if (metAverages.size() > 0) {
				//I keep the averages before release time only to count at least 5 averages in total
				//i < 5 because we get at most 5 averages taken before releaseTime
				for (int i=0; i< 5 && metAverages.size() > 5; i++) {
					int index = metAverages.size()-1;
					if (metAverages.get(index).getDateTaken().before(releaseTime)) {
						metAverages.remove(index);
					} else {
						break;
					}
				}
				List<double[]> winds = new ArrayList<double[]>();
				int hstab=0;
				int count=0;
				for (MetAverage avg : metAverages) {
					if (avg.getMetData() != null && avg.getMetData().getWindDirection() != null && avg.getMetData().getWindSpeed() != null) {
						if (metLocation == null) {
							metLocation = mapper.map(avg.getLocation(), PointDTO.class);
						}
						winds.add(new double[]{avg.getMetData().getWindSpeed(), avg.getMetData().getWindDirection()});
						hstab+=avg.getMetData().gethStability();
						count++;
					}
				}
				if(count > 0) {
					double[] avgWind = VectorUtils.sum(winds);
					if (winds.size() > 0) {
						double avgSpeed = avgWind[0]/winds.size();
						data.setWindSpeed(avgSpeed);
					} 
					data.setWindDirection(avgWind[1]);
					
					data.sethStability((int)(hstab/count));
					data.setLatestMetAverageTime(DateUtils.convertToTimezone(latest.getDateTaken(), currentSite.getTimeZone()));
					data.setMetLocation(metLocation);
				}
			}
		}
		
		if (metStationId!=null && metStationId == -1){
			ManualMetData manualMet = dataSourceManager.getManualMetData(getCurrentUser(), currentSite);
			if (manualMet != null && manualMet.getMetData() != null) {
				if (manualMet.getMetData().getWindSpeed() != null) {
					data.setWindSpeed(manualMet.getMetData().getWindSpeed());
					data.setHazmatWindspeed(FormatUtils.getErgHazmatWindspeed(manualMet.getMetData().getWindSpeed()));
				}
				if (manualMet.getMetData().getWindDirection() != null ){
					data.setWindDirection(manualMet.getMetData().getWindDirection());
				}
				data.sethStability(manualMet.getMetData().gethStability());
			}
			releaseTime = DateUtils.trimToMinutes(new Date());
		}
		
		if (releaseTime != null) {
			data.setReleaseTime(DateUtils.convertToTimezone(releaseTime, currentSite.getTimeZone()));
			if (location != null && location.getLatitude() != null && location.getLongitude() != null){
				ImmutablePair<LocalDateTime, LocalDateTime> sunriseSunsetTimes = DateUtils.getSunriseSunsetTimes(releaseTime,location.getLatitude().doubleValue(), location.getLongitude().doubleValue());
				LocalDateTime release = LocalDateTime.ofInstant(releaseTime.toInstant(), ZoneId.systemDefault());
				LocalDateTime start, end;
				ErgSpillTime inside, outside;

				if (sunriseSunsetTimes.left.isAfter(sunriseSunsetTimes.right)) {
					start = sunriseSunsetTimes.right;
					end = sunriseSunsetTimes.left;
					inside = ErgSpillTime.NIGHT;
					outside = ErgSpillTime.DAY;
				} else {
					start = sunriseSunsetTimes.left;
					end = sunriseSunsetTimes.right;
					inside = ErgSpillTime.DAY;
					outside = ErgSpillTime.NIGHT;
				}
				if (release.isAfter(start) && release.isBefore(end)) {
					data.setSpillTime(inside);
				} else {
					data.setSpillTime(outside);
				}
			}
		}
		
		data.setHazmatWindspeed(FormatUtils.getErgHazmatWindspeed(data.getWindSpeed()));
		return data;
	}

	@Override
	public SiteSettingsDTO getSiteSettings(Integer siteId) {
		Integer userId = getCurrentUser().getId();
		Site site = getCurrentSite();
		
		if(siteId!=null){
			if(!getCurrentUser().isSiteAdmin()){
				throw new RuntimeException("Operation not allowed");
			}
			site = siteManager.getSite(siteId);
			if(site==null){
				throw new RuntimeException("Data not found");
			}
			//userId = null;
		}
		
		SiteSettings siteSettings = siteManager.getSiteSettings(site);
		SiteSettingsDTO result =  mapOrNewInstance(siteSettings, SiteSettingsDTO.class);
		unitsManager.toCustom(result.getAlarmLevelsData(), site.getId(),userId);
		unitsManager.toCustom(result.getOverpressureIsopleths(), site.getId(),userId);
		unitsManager.toCustom(result.getThermalIsopleths(), site.getId(),userId);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void saveDefaultSensorAlarmValues(List<DefaultSensorAlarmValuesDTO> sensorAlarmValuesDTO) {
		sensorAlarmValuesDTO = (List<DefaultSensorAlarmValuesDTO>)HtmlUtils.escapeHtml(sensorAlarmValuesDTO);
		
		User currentUser = getCurrentUser();
		Site currentSite = currentUser.getCurrentSite();
		SiteSettings siteSettings = siteManager.getSiteSettings(currentSite);
		
		List<DefaultSensorAlarmValues> result =  mapper.mapAsList(sensorAlarmValuesDTO, DefaultSensorAlarmValues.class);
		Map<String,DefaultSensorAlarmValues> valuesMap = new HashMap<String,DefaultSensorAlarmValues>(); 
		for(DefaultSensorAlarmValues value : result) {
			valuesMap.put(value.getChannelTag().getChannelTag(), value);
			dataSourceManager.updateSensorsAlarmValuesForSiteAndChannelTag(currentSite, value.getChannelTag().getChannelTag(), value.getAlarmValues());
		}
		siteSettings.setSensorAlarmValues(valuesMap);
		siteManager.save(siteSettings);		
		
		//TODO: do we need to refresh alarms ?
	}
	
	@Override
	public void acknowledgeAlarms(List<Integer> alarmIds) {
		for(Integer id : alarmIds) {
			Alarm alarm = alarmManager.getAlarm(id);
			if(alarm.getAcknowledged()==false) {
				alarm.setAcknowledged(true);
				alarm.setAcknowledgedAt(new Date());
				alarm.setAcknowledgedBy(getCurrentUser());
				alarmManager.save(alarm);
			}
		}
	}
	
	@Override
	public String createCSVFile(Date csvDateReceived, Integer metId){
		if(csvDateReceived==null || metId==null || metId.intValue()<0 ){
			throw new RuntimeException("No Met Average Available !");
		}
		
		Calendar c = Calendar.getInstance();
		c.setTime(csvDateReceived);
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		csvDateReceived = c.getTime();	
		Date csvDate = DateUtils.convertFromTimezoneToUtc(csvDateReceived, getCurrentSite().getTimeZone());

		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		final String metHistoryOutPath = getBaseFileFolderForUserFiles("report")+"Met History "+format.format(csvDateReceived)+".csv";
		final Units units = unitsManager.getUnits(getCurrentSite().getId(),getCurrentUser().getId());
		
		try {
			return properties.getApplicationUrl() + Config.DOWNLOAD_FILE_SERVICE_PATH+"?action=downloadFile&downloadType=report&fileName=" 
				+ createMetHistoryCSV(csvDate, new Date((csvDate.getTime()+24*3600*1000)-1), units, metId, MetAverageType.FIVE_MINUTE_AVERAGE, metHistoryOutPath, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}
	
	@Override
	public String createCSVFile(Date start, Date end, Integer metId, MetAverageType averageType){
		if(start==null || end == null || metId==null || metId.intValue()<0 || averageType == null){
			throw new RuntimeException("No Met Average Available !");
		}
		Date startUtc = DateUtils.convertFromTimezoneToUtc(start, getCurrentSite().getTimeZone());
		Date endUtc = DateUtils.convertFromTimezoneToUtc(end, getCurrentSite().getTimeZone());

		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		final String metHistoryOutPath = getBaseFileFolderForUserFiles("report")+"Met History "+format.format(start)+ " to " + format.format(end) + " (" + averageType + ")" + ".csv";
		final Units units = unitsManager.getUnits(getCurrentSite().getId(),getCurrentUser().getId());
		
		try {
			return properties.getApplicationUrl() + Config.DOWNLOAD_FILE_SERVICE_PATH+"?action=downloadFile&downloadType=report&fileName=" 
				+ createMetHistoryCSV(startUtc, endUtc, units, metId, averageType, metHistoryOutPath, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}
	
	@Override
	public String createAlarmHistoryCSVFile(ArrayList<AlarmDTO> alarms){
		if(alarms==null || alarms.isEmpty() ){
			throw new RuntimeException("No Alarms to export !");
		}
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		final Date d = DateUtils.convertToTimezone(new Date(), getCurrentSite().getTimeZone());
		final String alarmHistoryOutputPath = getBaseFileFolderForUserFiles("report")+"AlarmHistory"+format.format(d)+".csv";
		final Units units = unitsManager.getUnits(getCurrentSite().getId(), getCurrentUser().getId());
		
		try {
			return properties.getApplicationUrl() + Config.DOWNLOAD_FILE_SERVICE_PATH+"?action=downloadFile&downloadType=report&fileName=" 
				+ createAlarmHistory(alarms, units, alarmHistoryOutputPath,false);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}
	
	
	
	private String createMetHistoryCSV(final Date startDate, final Date endDate, Units units, Integer metId, MetAverageType averageType,
			String metHistoryOutPath ,ZipOutputStream zos) throws IOException{
		
		List<MetStation> mets = new ArrayList<MetStation>(); 
		if(metId==null) {
			mets =dataSourceManager.getMetStations(getCurrentSite());
		}else {
			mets.add(dataSourceManager.getMetStation(metId));
		}
		CSVWriter writer = new CSVWriter(new FileWriter(metHistoryOutPath), ',');
		String[] entries = {"Date Taken", "Met Station",
				"Wind Speed ("+units.getUMLabel(UnitParam.IDP_MET_WINDSPEED)+")", 
				"Wind Direction ("+units.getUMLabel(UnitDesc.degfromN)+")",
				"Max Wind Speed ("+units.getUMLabel(UnitParam.IDP_MET_WINDSPEED)+")",
				"Temperature ("+units.getUMLabel(UnitParam.IDP_MET_AMBTEMP)+")",
				"Humidity ("+units.getUMLabel(UnitParam.IDP_MET_RELHUMID)+")",
				"Feels Like ("+units.getUMLabel(UnitParam.IDP_MET_AMBTEMP)+")", 
				"Solar ("+units.getUMLabel(UnitParam.IDP_MET_SOLARRAD)+")", 
				"Pressure ("+units.getUMLabel(UnitParam.IDP_MET_PRESSURE)+")", 
				"Total Rainfall ("+units.getUMLabel(UnitParam.IDP_MET_RAINFALL)+")",
				"Five Minute Rainfall ("+units.getUMLabel(UnitParam.IDP_MET_RAINFALL)+")",
				"H Stability", 
				"V Stability"};
		writer.writeNext(entries);
		
		SimpleDateFormat f1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		for(MetStation met : mets) {
			List<MetAverage> averages = dataSourceManager.getMetAverages(met
					, averageType, startDate, endDate);
			for(MetAverage avg:averages){
				MetAverageDTO averageDto = mapper.map(avg, MetAverageDTO.class);
				unitsManager.toCustom(averageDto.getMetData(), getCurrentSite().getId(),getCurrentUser().getId());
				averageDto.setDateTaken(DateUtils.convertToTimezone(averageDto.getDateTaken(), getCurrentSite().getTimeZone()));
				
				String[] entries1 = {f1.format(averageDto.getDateTaken()), met.getName(),
						units.format(UnitParam.IDP_MET_WINDSPEED, averageDto.getMetData().getWindSpeed()),
						units.format(UnitDesc.degfromN,averageDto.getMetData().getWindDirection()), 
						units.format(UnitParam.IDP_MET_WINDSPEED,averageDto.getMetData().getMaxWindSpeed()),
						units.format(UnitParam.IDP_MET_AMBTEMP,averageDto.getMetData().getTemperature()),
						units.format(UnitParam.IDP_MET_RELHUMID,averageDto.getMetData().getHumidity()),
						units.format(UnitParam.IDP_MET_AMBTEMP,averageDto.getMetData().getFeelsLike()),
						units.format(UnitParam.IDP_MET_SOLARRAD,averageDto.getMetData().getSolarRadiation()),
						units.format(UnitParam.IDP_MET_PRESSURE,averageDto.getMetData().getPressure()),
						units.format(UnitParam.IDP_MET_RAINFALL,averageDto.getMetData().getRainfall()),
						units.format(UnitParam.IDP_MET_RAINFALL,averageDto.getMetData().getRainRate()),
						averageDto.getMetData().gethStability()+"",
						averageDto.getMetData().getvStability()+""};
				writer.writeNext(entries1);
			}
		}
		
		writer.close();
		
		if(zos!=null)
			ZipUtils.addToZipFile(metHistoryOutPath, zos);
		
		return FilenameUtils.getName(metHistoryOutPath);
	}
	
	public static class Flags{
		boolean includeMap;
		boolean includeErgGuide;
		boolean includeMet;
		boolean includeSensors;
		boolean includeMetHistory;
		boolean includeAlarms;
		boolean includeAlarmHistory;
		boolean includeCSVImpactedPlaces;
		boolean includeKmz;
		boolean includeCorridorDetails;
		boolean includeScenarioInputs;
		boolean includeOutputSummary;
		boolean includeVerticalProfile;
		boolean includeReceptorImpactSummary;
		boolean includeReceptorImpactDetails;
		boolean includeMsdsHazmatInfo;
		boolean includeChemicalProperties;
		boolean includeSensorDataCSV;
		public boolean isIncludeMap() {
			return includeMap;
		}
		public void setIncludeMap(boolean includeMap) {
			this.includeMap = includeMap;
		}
		public boolean isIncludeErgGuide() {
			return includeErgGuide;
		}
		public void setIncludeErgGuide(boolean includeErgGuide) {
			this.includeErgGuide = includeErgGuide;
		}
		public boolean isIncludeMet() {
			return includeMet;
		}
		public void setIncludeMet(boolean includeMet) {
			this.includeMet = includeMet;
		}
		public boolean isIncludeMetHistory() {
			return includeMetHistory;
		}
		public void setIncludeMetHistory(boolean includeMetHistory) {
			this.includeMetHistory = includeMetHistory;
		}
		public boolean isIncludeAlarms() {
			return includeAlarms;
		}
		public void setIncludeAlarms(boolean includeAlarms) {
			this.includeAlarms = includeAlarms;
		}
		public boolean isIncludeAlarmHistory() {
			return includeAlarmHistory;
		}
		public void setIncludeAlarmHistory(boolean includeAlarmHistory) {
			this.includeAlarmHistory = includeAlarmHistory;
		}
		public boolean isIncludeCSVImpactedPlaces() {
			return includeCSVImpactedPlaces;
		}
		public void setIncludeCSVImpactedPlaces(boolean includeCSVImpactedPlaces) {
			this.includeCSVImpactedPlaces = includeCSVImpactedPlaces;
		}
		public boolean isIncludeKmz() {
			return includeKmz;
		}
		public void setIncludeKmz(boolean includeKmz) {
			this.includeKmz = includeKmz;
		}
		public boolean isIncludeCorridorDetails() {
			return includeCorridorDetails;
		}
		public void setIncludeCorridorDetails(boolean includeCorridorDetails) {
			this.includeCorridorDetails = includeCorridorDetails;
		}
		public boolean isIncludeScenarioInputs() {
			return includeScenarioInputs;
		}
		public void setIncludeScenarioInputs(boolean includeScenarioInputs) {
			this.includeScenarioInputs = includeScenarioInputs;
		}
		public boolean isIncludeOutputSummary() {
			return includeOutputSummary;
		}
		public void setIncludeOutputSummary(boolean includeOutputSummary) {
			this.includeOutputSummary = includeOutputSummary;
		}
		public boolean isIncludeVerticalProfile() {
			return includeVerticalProfile;
		}
		public void setIncludeVerticalProfile(boolean includeVerticalProfile) {
			this.includeVerticalProfile = includeVerticalProfile;
		}
		public boolean isIncludeReceptorImpactSummary() {
			return includeReceptorImpactSummary;
		}
		public void setIncludeReceptorImpactSummary(boolean includeReceptorImpactSummary) {
			this.includeReceptorImpactSummary = includeReceptorImpactSummary;
		}
		public boolean isIncludeReceptorImpactDetails() {
			return includeReceptorImpactDetails;
		}
		public void setIncludeReceptorImpactDetails(boolean includeReceptorImpactDetails) {
			this.includeReceptorImpactDetails = includeReceptorImpactDetails;
		}
		public boolean isIncludeMsdsHazmatInfo() {
			return includeMsdsHazmatInfo;
		}
		public void setIncludeMsdsHazmatInfo(boolean includeMsdsHazmatInfo) {
			this.includeMsdsHazmatInfo = includeMsdsHazmatInfo;
		}
		public boolean isIncludeChemicalProperties() {
			return includeChemicalProperties;
		}
		public void setIncludeChemicalProperties(boolean includeChemicalProperties) {
			this.includeChemicalProperties = includeChemicalProperties;
		}
		public boolean isIncludeSensors() {
			return includeSensors;
		}
		public void setIncludeSensors(boolean includeSensors) {
			this.includeSensors = includeSensors;
		}
		public boolean isIncludeSensorDataCSV() {
			return includeSensorDataCSV;
		}
		public void setIncludeSensorDataCSV(boolean includeSensorDataCSV) {
			this.includeSensorDataCSV = includeSensorDataCSV;
		}
		
		
	}
	
	@Override
	public boolean resetTemplate(Integer siteId,TemplateType type) {
		if(!getCurrentUser().isSaferAdmin()){
			return false;
		}
		
		Site site = siteManager.getSite(siteId);
		if(site==null){
			return false;
		}
		
		String baseSiteFolder = ServerUtils.getBaseFileFolderForSiteFiles(true, site.getId(), properties);
		
		boolean quickTemplateExists = Files.isRegularFile(Paths.get(baseSiteFolder+Config.QUICK_TEMPLATE));
		boolean ergTemplateExists = Files.isRegularFile(Paths.get(baseSiteFolder+Config.ERG_TEMPLATE));
		boolean scenarioTemplateExists = Files.isRegularFile(Paths.get(baseSiteFolder+Config.SCENARIO_TEMPLATE));
	
		switch(type) {
		case QUICK:
			if(quickTemplateExists) {
				return FileUtils.deleteQuietly(new File(baseSiteFolder+Config.QUICK_TEMPLATE));
			}
			break;
		case ERG:
			if(ergTemplateExists) {
				return FileUtils.deleteQuietly(new File(baseSiteFolder+Config.ERG_TEMPLATE));
			}
			break;
		case SCENARIO:
			if(scenarioTemplateExists) {
				return FileUtils.deleteQuietly(new File(baseSiteFolder+Config.SCENARIO_TEMPLATE));
			}
			break;
		default:
			break;
		
		}
		return true;
	}
	
	@Override
	public boolean resetCustomMap(Integer siteId) {
		if(!getCurrentUser().isSaferAdmin()){
			return false;
		}
		
		Site site = siteManager.getSite(siteId);
		if(site==null){
			return false;
		}
		String baseSiteFolder = ServerUtils.getBaseFileFolderForSiteFiles(true, site.getId(), properties);
		
		File customMapFolder = new File(baseSiteFolder+Config.CUSTOM_MAP_FOLDER);
		File customMapFile = new File(baseSiteFolder+Config.CUSTOM_MAP);
		
		try {
			if(customMapFolder.isDirectory()) {
				FileUtils.deleteDirectory(customMapFolder);
			}
			if(customMapFile.isFile()) {
				customMapFile.delete();
			}
			
			site.setCustomMap(null);
			siteManager.save(site);
		}catch(Exception e) {
			return false;
		}
		return true;
	}
	
	@Override
	public boolean isCustomMapAvailableForDownload(Integer siteId) {
		if(!getCurrentUser().isSaferAdmin()){
			return false;
		}
		Site site = siteManager.getSite(siteId);
		if(site==null){
			return false;
		}
		String baseSiteFolder = ServerUtils.getBaseFileFolderForSiteFiles(true, site.getId(), properties);
		File customMapFile = new File(baseSiteFolder+Config.CUSTOM_MAP);
		return customMapFile.isFile();
	}
	
	@Override
	public String createReport(String base64ScreenShot, String base64MetWidgetdata, String base64SemaphoreData, String base64PlumeVerticalProfile, String base64ErgFormData, String base64MetBoxData, String base64ScenarioSummary, MetAverageDTO average, LightningInfoDTO lightning, List<Pair<String, String>> lightningInfoMap, 
			Set<ReportExtraOptionType> extra,ImpactedPoisCalculatedDTO data, String impactedSorting, boolean shareReport, String shareWith) throws RuntimeException, UnsupportedEncodingException {
		
//		base64ScreenShot = HtmlUtils.escapeHtml(base64ScreenShot);
//		base64MetWidgetdata = HtmlUtils.escapeHtml(base64MetWidgetdata);
//		base64PlumeVerticalProfile = HtmlUtils.escapeHtml(base64PlumeVerticalProfile);
//		
//		base64ErgFormData= HtmlUtils.escapeHtml(base64ErgFormData);
//		base64MetBoxData = HtmlUtils.escapeHtml(base64MetBoxData);
//		base64ScenarioSummary= HtmlUtils.escapeHtml(base64ScenarioSummary);
//		average= (MetAverageDTO) HtmlUtils.escapeHtml(average);
//		data= (ImpactedPoisCalculatedDTO) HtmlUtils.escapeHtml(data);
		impactedSorting= HtmlUtils.escapeHtml(impactedSorting);
		shareWith= HtmlUtils.escapeHtml(shareWith);
		
		boolean isEvent = data.isErg()||data.isScenario();
		boolean isSal = false;
		boolean isFireExplosion = false; 
		ScenarioRun scenarioRun = null;
		ScenarioRunDTO scenarioRunDTO = null;
		try {
			if(data.isScenario()) {
				scenarioRun = scenarioManager.getScenarioRun(data.getScenarioId());
				scenarioRunDTO = getScenarioOutput(scenarioRun);
				isSal = scenarioRun.getScenarioType() == ScenarioType.SAL;
				isFireExplosion = scenarioRun.getScenarioType().isFire() ||scenarioRun.getScenarioType().isExplosion();
				
				if(!isSal && !isFireExplosion) {
					double keyIsopleths[]  = getScenarioKeyIsopleths(scenarioRun);
					double isopleths[] = getScenarioIsopleths(scenarioRun);
						
					final String plumeInFilePath = ServerUtils.getBaseFileFolderForScenarioRun(scenarioRun,scenarioRun.getUser().getId(),true,properties)+Config.PLUME_DAT;
					String plumeData = FileUtils.readFileToString(new File(plumeInFilePath));
					Plume plume = PlumeUtils.parseDispersionPlume( plumeData,  keyIsopleths==null?isopleths:keyIsopleths, getScenarioRunDTO(scenarioRun).getLocation());
					Double molarFraction = getMolarFraction(keyIsopleths, isopleths);
					
					if(data.getImpactedPois()!=null) {
						for(int i=0;i<data.getImpactedPois().size();i++) {
							RTPointOfInterestDTO rtpoi = new RTPointOfInterestDTO();
							SaferPointOfInterest spoi = data.getImpactedPois().get(i);
							rtpoi = mapper.map(spoi, RTPointOfInterestDTO.class);
							if(spoi instanceof ZonePointOfInterestDTO) {
								rtpoi.setType(POIType.DRAWING);
							}
							plume.CalcInfImpact(rtpoi,  keyIsopleths==null?isopleths[0]:keyIsopleths[0], 23, molarFraction);
							data.getImpactedPois().set(i, rtpoi);
						}
					}
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}

		
		Date refDate = null;
		Date releaseDate = null;
		boolean refIsUtc = false;
		boolean releaseIsUtc = false;
		
		if (data.isErg()) {
			refDate = data.getErgEvent().getCreatedDate();
			releaseDate = data.getErgEvent().getReleaseTime();
			refIsUtc = true;
			releaseIsUtc = false;
		} else if (data.isScenario()) {
			refDate = scenarioRun.getCreatedDate();
			releaseDate = scenarioRun.getReleaseTime();
			refIsUtc = true;
			releaseIsUtc = true;
		} else if (average != null) {
			refDate = average.getDateTaken();
			releaseDate =average.getDateTaken();
			refIsUtc = false;
			releaseIsUtc = false;
		}
		if (refDate == null) {
			refDate = new Date();
			releaseDate = new Date();
			refIsUtc = true;
			releaseIsUtc = true;
		} 
		
		if(!refIsUtc) {
			refDate = DateUtils.convertFromTimezoneToUtc(refDate, getCurrentSite().getTimeZone()); 
		}
		if(!releaseIsUtc) {
			releaseDate =  DateUtils.convertFromTimezoneToUtc(releaseDate, getCurrentSite().getTimeZone());;
		}
		
		final SimpleDateFormat refDateSdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		String refDateFormatted = refDateSdf.format(DateUtils.convertToTimezone(refDate,getCurrentSite().getTimeZone()));
		
		final String suffix = isEvent?"Event":"Quick";
		final String outFileName = suffix+"Report-"+refDateFormatted;
		final String basePath = getBaseFileFolderForUserFiles("report");
		final String reportTemplateOutFilePath = basePath+outFileName+".docx";
		final SiteSettings siteSettings = siteManager.getSiteSettings(getCurrentSite());
		final ReportFileType reportType = siteSettings.getReportFileType();
		
		final Flags f = new Flags();
		f.includeErgGuide = extra.contains(ReportExtraOptionType.ERG_GUIDE);
		f.includeMetHistory = extra.contains(ReportExtraOptionType.MET_HISTORY);
		f.includeAlarmHistory = extra.contains(ReportExtraOptionType.ALARM_HISTORY);
		f.includeCSVImpactedPlaces = extra.contains(ReportExtraOptionType.CSV_IMPACTED_PLACES) || extra.contains(ReportExtraOptionType.CSV_DOWNWIND_PLACES);
		f.includeKmz = extra.contains(ReportExtraOptionType.KMZ);
		f.includeSensorDataCSV = extra.contains(ReportExtraOptionType.SENSOR_READINGS_CSV);
		f.includeCorridorDetails = extra.contains(ReportExtraOptionType.CORRIDOR);
		f.includeMet = extra.contains(ReportExtraOptionType.MET);
		f.includeAlarms = extra.contains(ReportExtraOptionType.ALARMS);
		f.includeMap = extra.contains(ReportExtraOptionType.MAP_VIEW);
		
		f.includeScenarioInputs = true;
		f.includeOutputSummary = !isSal && !isFireExplosion;
		f.includeVerticalProfile = extra.contains(ReportExtraOptionType.VERTICAL_PROFILE);
		f.includeReceptorImpactSummary = extra.contains(ReportExtraOptionType.RECEPTOR_IMPACT_SUMMARY);
		f.includeReceptorImpactDetails = extra.contains(ReportExtraOptionType.RECEPTOR_IMPACT_DETAILS);
		f.includeMsdsHazmatInfo = extra.contains(ReportExtraOptionType.MSDS_HAZMAT_INFO);
		f.includeChemicalProperties = extra.contains(ReportExtraOptionType.CHEMICAL_PROPERTIES);
		f.includeSensors = extra.contains(ReportExtraOptionType.SENSOR_READINGS);
				
		boolean includeDocReport = isEvent || f.includeMet || f.includeAlarms || f.includeCorridorDetails || f.includeMap;
		final Units units = unitsManager.getUnits(getCurrentSite().getId(),getCurrentUser().getId());
		String pdfReportFileName = null;
		

	
		
		if (includeDocReport || f.includeKmz || f.includeCSVImpactedPlaces) {
			if (data.getImpactedPois() == null) {
				data.setImpactedPois(new ArrayList<SaferPointOfInterest>());
			}
			
			for(SaferPointOfInterest poi : data.getImpactedPois()) {
				Double value = units.toCustom( UnitParam.IDP_OUTPUT_DISTANCE, poi.getDistance());
				String formatted = units.formatWithUM(UnitParam.IDP_OUTPUT_DISTANCE,value);
				poi.setDistanceFormatted(formatted);
				poi.setUiImpactType(FormatUtils.createUiImpactType(poi.getImpactType(), data.getErgEvent(), data.getScenarioId()));
			}
		}
		if(includeDocReport){
			String templateName = "";
			if(data.isErg()) {
				templateName = Config.ERG_TEMPLATE;
			}else if(data.isScenario()) {
				templateName = Config.SCENARIO_TEMPLATE;
			}else {
				templateName = Config.QUICK_TEMPLATE;;
			}
			fillTemplate(base64ScreenShot, base64MetWidgetdata,base64SemaphoreData,  base64PlumeVerticalProfile, average,lightning,lightningInfoMap, data, impactedSorting, 
					siteSettings,templateName, reportTemplateOutFilePath , f, scenarioRun, scenarioRunDTO);
			if(ReportFileType.PDF.equals(reportType)){
				pdfReportFileName = pdfConverter.convertWordToPDF(reportTemplateOutFilePath, basePath);
			}
		}
		
		final boolean isZip = f.includeErgGuide || f.includeMetHistory || f.includeAlarmHistory || f.includeCSVImpactedPlaces || f.includeKmz || f.includeSensorDataCSV;
		
		if(isZip){
			FileOutputStream fos = null;
			ZipOutputStream zos = null;
			try {
				fos = new FileOutputStream( basePath+outFileName+".zip");
				zos = new ZipOutputStream(fos);
				if(includeDocReport){
					ZipUtils.addToZipFile((pdfReportFileName==null?reportTemplateOutFilePath:pdfReportFileName), zos);
				}
				
				if(f.includeErgGuide) {
					final String ergGuideName;
					if (data.getErgResults()!=null)  {
						ergGuideName = data.getErgResults().getGuide();
					} else {
						ergGuideName = data.getScenarioErgGuide();
					}
					if(ergGuideName!=null){
						String path = pdfGuidePath.getFile().getCanonicalPath()+"/"+ergGuideName+Config.GUIDE_PDF_EXTENSION;
						if (!Files.exists(Paths.get(path))){
							path = properties.getBaseFilesPath() +"/ergguide/Pdf/"+data.getErgResults().getGuide()+Config.GUIDE_PDF_EXTENSION;
						}
						ZipUtils.addToZipFile(path, zos);
					}
				}
				
				Date startDate = new Date();
				Date endDate = new Date();;
				
				if(data.isScenario()) {
					startDate.setTime(releaseDate.getTime());
					endDate.setTime(releaseDate.getTime()+2*60*60*1000);
				}else {
					startDate.setTime(releaseDate.getTime()-12*60*60*1000);
					endDate.setTime(releaseDate.getTime()+12*60*60*1000);	
				}
				
				
				Date startDate24h = new Date();
				Date endDate24h = new Date();;
				
				startDate24h.setTime(releaseDate.getTime()-24*60*60*1000);
				endDate24h.setTime(releaseDate.getTime()+24*60*60*1000);
				
				
				
				if(f.includeMetHistory){
					try {
						String filename = "MeteorologyData-"+ refDateFormatted  +".csv";
						createMetHistoryCSV(startDate, endDate, units, null, MetAverageType.FIVE_MINUTE_AVERAGE, basePath+filename, zos);
					}catch(Exception e) {
						e.printStackTrace();
					}
						
				}
				
				if(f.includeAlarmHistory){
					String filename = "AlarmHistory-"+ refDateFormatted  +".csv";
					createAlarmHistory(startDate24h,endDate24h,units,basePath+filename,zos);
				}
				
				if(f.includeSensorDataCSV) {
					try{
						String filename = "SensorData-"+ refDateFormatted  +".csv";
						if(scenarioRun!=null) {
							createSensorDataCSV(scenarioRun, units,basePath+filename, zos);
						}else {
							createSensorHistoryCSV(startDate, endDate, mapper.mapAsList(dataSourceManager.getSensorsEnabled(getCurrentSite()), SensorDTO.class),basePath+filename, zos, true);
						}
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
				
				if(f.includeCSVImpactedPlaces){
					try {
						String filename = "DownwindPlaces.csv";
						if(data.isScenario() || data.isErg()) {
							filename = "ImpactedPlaces.csv";
						}
						
						final String csvImpactedOutputPath = basePath+filename;
						final CSVWriter writer = new CSVWriter(new FileWriter(csvImpactedOutputPath), ',');
						final String[] entriesERG = {"Impact","Name", "Type", "Distance (" + units.getUMLabel(UnitParam.IDP_OUTPUT_DISTANCE) + ") ", "Address", "Phone"};
						final String[] entriesCoridor = {"Name", "Type", "Distance (" + units.getUMLabel(UnitParam.IDP_OUTPUT_DISTANCE) + ") ", "Address", "Phone"};
						final String[] entriesUpwind = {"Name", "Type", "Distance (" + units.getUMLabel(UnitParam.IDP_OUTPUT_DISTANCE) + ") ", "Chemical"};
						final String[] entriesScenario = {"Name", "Phone", "Type", "Impact Level", "Peak Conc.","First Impact","Distance (" + units.getUMLabel(UnitParam.IDP_OUTPUT_DISTANCE) + ") ", "Address"};
						final String[] entriesScenarioFireExpl = {"Name", "Phone", "Type", "Impact Level", "Distance (" + units.getUMLabel(UnitParam.IDP_OUTPUT_DISTANCE) + ") ", "Address"};
						SimpleDateFormat formatImpactTime = new SimpleDateFormat("h:mm a");
						
						if(data.isErg()){
							writer.writeNext(entriesERG);
						}else if(data.isScenario()){
							if(isFireExplosion) {
								writer.writeNext(entriesScenarioFireExpl);
							}else {
								writer.writeNext(entriesScenario);
							}
						}else if (data.isDownwind()){
							List<String> entriesList = Arrays.asList(entriesCoridor);
							writer.writeNext(entriesList.toArray(new String[entriesList.size()]));
						} else {
							writer.writeNext(entriesUpwind);
						}
						
						for(SaferPointOfInterest al:data.getImpactedPois()){
							String address = null;
							String phone = null;
							String chemical = null;
	
							if (al instanceof PointOfInterestDTO) {
								address = ((PointOfInterestDTO)al).getAddress();
								phone = ((PointOfInterestDTO)al).getPhone();
								address = (address==null?"":address);
								phone = (phone==null?"":phone);
							}else if(al instanceof ZonePointOfInterestDTO){
								address ="";
								phone ="";
							}
							else if(al instanceof EmissionSourceDTO){
								ErgChemicalIdDTO chemicalId =((EmissionSourceDTO)al).getChemicalId();
								if (chemicalId != null && chemicalId.getErgChemicalName() != null) {
									chemical = chemicalId.getErgChemicalName();
								} else {
									chemical = "";
								}
							}
							
							if(data.isErg()){
								String[] entries2 = {al.getUiImpactType(),al.getName() ,al.getTypeName() ,al.getDistanceFormatted() ,address, phone};
								writer.writeNext(entries2);
							}else if(data.isScenario() && al instanceof RTPointOfInterestDTO) {
								 RTPointOfInterestDTO rtpoi = ((RTPointOfInterestDTO)al);
								 
								 String timeFormatted = getTimeFormattedAtMinute(formatImpactTime,scenarioRun.getReleaseTime(), getCurrentSite().getTimeZone(), rtpoi.firstImpactMinute * 5 ) + " ( " + rtpoi.firstImpactDuration*5 + " min) " ;
								 Double value = units.toCustom( UnitParam.IDP_OUTPUT_DISTANCE, rtpoi.getDistance());
								 String distanceFormatted = units.format(UnitParam.IDP_OUTPUT_DISTANCE,value);
									
								 String[] entries2 = {al.getName() , phone, al.getTypeName() , al.getUiImpactType(), units.format(UnitParam.IDP_LIMIT_T, rtpoi.outdoorPeakConc), timeFormatted  , distanceFormatted,address };
								 writer.writeNext(entries2);
							}else if(data.isScenario() && isFireExplosion) {
								 Double value = units.toCustom( UnitParam.IDP_OUTPUT_DISTANCE, al.getDistance());
								 String distanceFormatted = units.format(UnitParam.IDP_OUTPUT_DISTANCE,value);
								 String[] entries2 = {al.getName() , phone, al.getTypeName() , al.getUiImpactType(),  distanceFormatted,address };
								 writer.writeNext(entries2);
							}else if (al instanceof PointOfInterestDTO){
								String[] entries2 = {al.getName() ,al.getTypeName() ,al.getDistanceFormatted() ,address, phone};
								writer.writeNext(entries2);
							} else {
								String[] entries2 = {al.getName() ,al.getTypeName() ,al.getDistanceFormatted() , chemical};
								writer.writeNext(entries2);
							}
						}
						
						writer.close();
						
						ZipUtils.addToZipFile(csvImpactedOutputPath, zos);
					}catch(Exception e) {
						e.printStackTrace();
					}
				}
				
				if (f.includeKmz) {
					String kmzOutputFile = basePath + outFileName + ".kmz";
					String mainFilePictureData = null;
					String mainPictureData = null;
					if (data.isScenario() && base64ScenarioSummary != null) {
						mainPictureData = base64ScenarioSummary;
					} else if (data.getErgEvent() != null && base64ErgFormData != null){
						mainPictureData = base64ErgFormData;
					} else if (data.getErgEvent() == null && base64MetBoxData != null) {
						mainPictureData = base64MetBoxData;
					}
					if (mainPictureData != null) {
						mainFilePictureData = basePath + "mainpicture.png";
						byte[] bytes = Base64.getDecoder().decode( mainPictureData.substring(mainPictureData.indexOf(',')+1));
						try (OutputStream stream = new FileOutputStream(mainFilePictureData)) {
						    stream.write(bytes);
						}
					}
					createSaferKmz(kmzOutputFile, data.getImpactedPois(), data, scenarioRun, mainFilePictureData);
					ZipUtils.addToZipFile(kmzOutputFile, zos);
					new File(kmzOutputFile).delete();
					if (mainFilePictureData != null) {
						new File(mainFilePictureData).delete();
					}
				}
				
				zos.flush();
			} catch (Exception e) {
				e.printStackTrace();
				//throw new RuntimeException(e.getMessage());
			}
			finally{
				if(fos!=null){
					try {zos.close();} catch (IOException e) { e.printStackTrace();}
					try {fos.close();} catch (IOException e) { e.printStackTrace();}
				}
			}
		}
		
		String fileName = outFileName + (isZip?".zip":(pdfReportFileName==null?".docx":".pdf"));
		
		if(shareReport && !StringUtils.isEmpty(shareWith)) {
			shareReport(shareWith, fileName, data, scenarioRun, scenarioRunDTO, units);
			saveShareWithUserSetting(shareWith);
		}
		
		return properties.getApplicationUrl() + Config.DOWNLOAD_FILE_SERVICE_PATH+"?action=downloadFile&downloadType=report&fileName=" + fileName;
	}

	private void createSensorDataCSV(ScenarioRun scenarioRun, Units units, String path, ZipOutputStream zos) throws IOException {
		List<SensorModeling> sensors = scenarioRun.getSensors();
		List<SensorAverage> avgs = new ArrayList<>();
		for (SensorModeling sensor : sensors) {
			if(sensor.isSelectedInInput() && !sensor.isRejectedbyModel()) {
				for (SensorAverageModeling avg : sensor.getAverages()) {
					avgs.add(avg);
				}
			}
		}
		if(scenarioRun.getManualSensors()!=null) {
			for(Entry<Integer, ManualSensor> entry : scenarioRun.getManualSensors().entrySet()) {
				ManualSensor manualSensor = entry.getValue();
				if(manualSensor.isSelected()) {
					Sensor s = new Sensor();
					s.setName(manualSensor.getName());
					s.setLocation(manualSensor.getLocation());
					s.setType(SensorType.MANUAL);
	
					Map<Integer,ManualSensorAverage> manualAverages = entry.getValue().getAverages();
					for(int i=1;i<=120;i++) {
						if(manualAverages.containsKey(i)) {
							SensorAverage avg = new SensorAverage();
							avg.setSensor(s);
							avg.setDateTaken(DateUtils.addMinutes(scenarioRun.getReleaseTime(), i-1));
							avg.setValue(manualAverages.get(i).getValue());
							avg.setLocation(manualSensor.getLocation());
							avgs.add(avg);
						}
					}
				}
			}
		}
		craeteSensorDataCSV(avgs, units, path, zos);
	}

	private void craeteSensorDataCSV(List<SensorAverage> avgs, Units units, String path, ZipOutputStream zos) throws IOException {
		CSVWriter writer = new CSVWriter(new FileWriter(path), ',');
		String[] entries = { "Date Taken", "Location", "Sensor", "Type", "Chemical", "Device ID", "Value", "Unit" };
		writer.writeNext(entries);

		SimpleDateFormat f1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");

		for (SensorAverage avg : avgs) {

			avg.setDateTaken(DateUtils.convertToTimezone(avg.getDateTaken(), getCurrentSite().getTimeZone()));

			Sensor s = avg.getSensor();

			PointConverter pc = new PointConverter();
			String[] entries1 = { 
					f1.format(avg.getDateTaken()), 
					pc.convertFrom(avg.getLocation(), null).toString(),
					SensorUtils.cleanName(s.getName()), 
					s.getType().toString(),
					s.getChemical() != null ? s.getChemical().getName() : "",
					s.getSensorRae() != null ? s.getSensorRae().getSerialNumber() : (s.getSensorGroup() !=null ? s.getSensorGroup().getName() : ""),
					SensorType.GENERIC.equals(s.getType()) || SensorType.EC.equals(s.getType()) || SensorType.MANUAL.equals(s.getType()) ? units.convertAndFormatAllowNull(UnitParam.IDP_SENSOR_READING, avg.getValue()) : units.format(UnitParam.IDP_SENSOR_READING, avg.getValue()),
					SensorType.GENERIC.equals(s.getType()) || SensorType.EC.equals(s.getType()) || SensorType.MANUAL.equals(s.getType()) ? units.getUMLabel(UnitParam.IDP_SENSOR_READING) : s.getUnitLabel() 
							};
			writer.writeNext(entries1);
		}

		writer.close();

		if (zos != null) {
			ZipUtils.addToZipFile(path, zos);
		}
	}

	//this should be in scenarioUtils but cannot be because some date functions are not available in client context
	private static String getTimeFormattedAtMinute(SimpleDateFormat format, Date releaseTime, String timezone, int minute) {
		 Date scenarioReleaseTime = DateUtils.convertToTimezone(DateUtils.roundToNextFiveMinutes(releaseTime), timezone);
		 String timeFormatted = format.format(DateUtils.addMinutes(scenarioReleaseTime,  minute));
		 return timeFormatted;
	}
	
	private String createAlarmHistory(List<?extends AlarmDtoI> alarms,final Units units,final String alarmHistoryOutputPath, boolean isUtc) throws IOException{
		SimpleDateFormat f1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		CSVWriter writer = new CSVWriter(new FileWriter(alarmHistoryOutputPath), ',');
		String[] entries = {"Active","Alarm Type", "Device Name", "Start Time", "Duration", "Max value", "Acknowledged at", "Acknowledged by"};
		writer.writeNext(entries);
		
		for(AlarmDtoI al:alarms){
			Date ackDate = al.getAcknowledgedAt();
			Date startDate = al.getStartDate();
			
			if(isUtc){
				ackDate = DateUtils.convertToTimezone(ackDate, getCurrentSite().getTimeZone());
				startDate = DateUtils.convertToTimezone(startDate, getCurrentSite().getTimeZone());
			}
			String deviceName = al.getDeviceName();
			if(al instanceof Alarm) {
				Alarm alarm = (Alarm)al;
				deviceName = AlarmManager.getAlarmDeviceName(alarm.getDaq(),alarm.getMetStation(),alarm.getSensor(), alarm.getSensorRae());
			}
			String[] entries1 = {al.getActive()+"",al.getType().toString() ,deviceName ,
					f1.format(startDate) ,al.getActivityFormatted()
					,FormatUtils.formatAlarmValue(al.getMaxValue(),al.getType(),al.getUnitLabel(), units)
					,ackDate!=null?f1.format(ackDate):"", al.getAcknowledgedByName()};
			writer.writeNext(entries1);
		}
		writer.close();
		return FilenameUtils.getName(alarmHistoryOutputPath);
	}
	private void createAlarmHistory(final Date startDate, final Date endDate, final Units units,final String alarmHistoryOutputPath
			,ZipOutputStream zos) throws IOException {
		List<Alarm> alarms = alarmManager.getAlarmHistory(getCurrentSite(),startDate, endDate);
		
		createAlarmHistory(alarms, units, alarmHistoryOutputPath, true);
		if(zos!=null){
			ZipUtils.addToZipFile(alarmHistoryOutputPath, zos);
		}
	}

	private void saveShareWithUserSetting(String shareWith) {
		User currentUser = getCurrentUser();
		settingManager.saveSetting(SettingParam.SHARE_REPORT_WITH, shareWith, currentUser);
	}

	public void createSaferKmz(String kmzOutputFile, final List<SaferPointOfInterest> impactedPois, ImpactedPoisCalculatedDTO data, ScenarioRun scenarioRun, String meteoWidgetImagePath) {
		SaferKmz kmz = new SaferKmz(kmzOutputFile, ctx);
		User currentUser = getCurrentUser();
		Site currentSite = getCurrentSite();
		List<PointOfInterest> eventPois;
		if (data.isScenario()) {
			eventPois = poisManager.getPoiRepository().findBySiteAndHiddenFalseAndGroupId(currentSite, scenarioRun.getGroupId());
		} else if (data.getErgEvent() != null) {
			eventPois = poisManager.getPoiRepository().findBySiteAndHiddenFalseAndGroupId(currentSite, data.getErgEvent().getGroupId());
		} else {
			eventPois = new ArrayList<PointOfInterest>();
		}
		
		//impacted pois means emission source for upwind corridor or sal.
		//for scenario output emission source come from client in markers list
		if (data.getErgEvent() != null || data.isDownwind() || data.getScenarioType() != ScenarioType.SAL) {
			kmz.addPois(impactedPois, poisManager.getPoiRepository().findBySiteAndHiddenFalseAndGroupIdNull(currentSite),
				eventPois);
			if (!data.isScenario()) {
				kmz.addEmissionSources(new ArrayList<SaferPointOfInterest>(), emissionManager.getEmissionRepository().findBySiteAndHiddenFalseAndLocationNotNull(currentSite));
			}
		} else {
			if (!data.isScenario()) {
				kmz.addEmissionSources(impactedPois, emissionManager.getEmissionRepository().findBySiteAndHiddenFalseAndLocationNotNull(currentSite));	
			}
			kmz.addPois(new ArrayList<SaferPointOfInterest>(), poisManager.getPoiRepository().findBySiteAndHiddenFalseAndGroupIdNull(currentSite),
					eventPois);
		}
		
		//center
		boolean centerVisible = data.getErgEvent()!=null || data.isScenario() || getMapLayersSettings(currentUser).contains(MapLayerType.CORRIDOR);
		String name = null;
		if (data.isScenario()) {
			name = scenarioRun.getName();
		} else if (data.getErgEvent() != null) {
			if (data.getErgEvent().getErgType() == ErgType.HAZMAT && data.getErgEvent().getErgChemicalId() != null ) {
				name = data.getErgEvent().getErgChemicalId().getErgChemicalName();
			} else {
				name = data.getErgEvent().getName();
			}
		} else {
			name = data.isDownwind() ? "Release Source" : "Receptor Location";
		}
		kmz.addCenter(data.getCenter(), name, data.isErg() || data.isScenario(), currentSite.getDefaultZoom(), centerVisible);
		
		
		if (data.getErgEvent() == null && !data.isScenario()) {//met stations are not visible in event
			Setting primaryMet = settingManager.getSetting(SettingParam.PRIMARY_MET_STATION, getCurrentUser(), currentSite);
			HashMap<MetAverageType, Map<Integer, MetAverageDTO>> avgs = new HashMap<MetAverageType, Map<Integer,MetAverageDTO>>();
			SiteSettingsDTO siteSettings = getSiteSettings(null);
			getRegularUpdateMetAverages(null, null, avgs, siteSettings.getMetAverageType(), false);
			kmz.addMetStations(dataSourceManager.getMetStations(currentSite), 
					primaryMet != null ? primaryMet.getIntValue() : null, 
					avgs.get(siteSettings.getMetAverageType()),
					unitsManager.getUnits(currentSite.getId(),getCurrentUser().getId()),
					currentSite.getTimeZone());
		}
		
		if (data.getPolylines() != null && !data.getPolylines().isEmpty()){
			kmz.addPolylines(data.getPolylines(), data.getKmlGroupVisibility() != null ? data.getKmlGroupVisibility() : new HashMap<String, Boolean>());
		}
		
		if (data.getPolygons() != null && !data.getPolygons().isEmpty()){
			kmz.addPolygons(data.getPolygons(), data.getKmlGroupVisibility() != null ? data.getKmlGroupVisibility() : new HashMap<String, Boolean>());
		}
		
		if (data.getPlume() != null && !data.getPlume().isEmpty()) {
			kmz.addPlume(data.getPlume());
		}
		
		if (meteoWidgetImagePath != null) {
			kmz.addMainPicture(meteoWidgetImagePath);
		}
		
		if (data.getMarkers() != null && !data.getMarkers().isEmpty()){
			kmz.addMarkers(data.getMarkers(), data.getKmlGroupVisibility() != null ? data.getKmlGroupVisibility() : new HashMap<String, Boolean>());
		}
		
		if (data.getKmls() != null && !data.getKmls().isEmpty()){
			kmz.addKmls(data.getKmls());
		}
		kmz.createKmz();
	}

	private void shareReport(String shareWith, String fileName, ImpactedPoisCalculatedDTO data, ScenarioRun scenarioRun, ScenarioRunDTO scenarioRunDTO, final Units units) {
		try {
			Map<String,Object> context = new HashMap<String, Object>();
			
			User currentUser = getCurrentUser();
			context.put("scenarioRun", scenarioRun);
			context.put("scenarioRunDTO", scenarioRunDTO);
			context.put("user", currentUser);
			context.put("isErg", data.isErg());
			context.put("erg", data.getErgEvent());
			context.put("dateTime",DateUtils.convertToTimezone(new Date(), getCurrentSite().getTimeZone()));
			context.put("locationLatLng", data.getCenter());
			context.put("locationAddress", getAddressFromPosition(data.getCenter()));
			context.put("downwind", data.isDownwind());
			context.put("dateTime",DateUtils.convertToTimezone(new Date(), getCurrentSite().getTimeZone()));
			context.put("applicationUrl", properties.getApplicationUrl());
			context.put("units", units);
			
			Map<String,Object> ergFields = fillTemplateErgFields(data, units, data.getErgResults());
			context.putAll(ergFields);
			
			BeansWrapper wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();
			TemplateHashModel staticModels = wrapper.getStaticModels();
			TemplateHashModel formatUtils = (TemplateHashModel) staticModels.get("com.safer.one.gwt.shared.FormatUtils");  
			context.put("FormatUtils", formatUtils);
			 
			TemplateHashModel enumModels = wrapper.getEnumModels();
			TemplateHashModel unitParamEnums = (TemplateHashModel) enumModels.get("com.safer.one.gwt.shared.enums.UnitParam"); 
			context.put("UnitParam", unitParamEnums);

			String templateName = "emailReportTemplate.html";
			if(data.isScenario()) {
				templateName = "emailReportTemplateScenario.html";
			}
			freeMarkerConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.IGNORE_HANDLER);
			String subject =  currentUser.getFirstName() + " " + currentUser.getLastName() + " has sent you a SAFER One RT " + ((data.isErg()||data.isScenario())?"Event Report":"Quick Report");
			final String baseFolderPath = ServerUtils.getBaseFileFolderForUserFiles("report", false, currentUser.getId(), properties);
			final String filePath = baseFolderPath + fileName;
						
			String[] emails =  shareWith.split("[;,]");
			for(String email : emails) {
				email = email.trim();
				if(!email.matches(FieldVerifier.EMAIL_PATTERN)) {
					continue;
				}
				User shareUser = userManager.getUser(email);
				
				Integer eventId = null;
				if(data.isErg() && data.getErgEvent()!=null) {
					eventId = data.getErgEvent().getId();
				}
				if(data.isScenario()) {
					eventId = scenarioRun.getId();
				}
				boolean isSaferUser = eventId != null  && shareUser!=null && !shareUser.getEmail().equals(currentUser.getEmail());
				if(isSaferUser) {
					eventManager.shareEvent(eventId, data.isErg() && data.getErgEvent()!=null, getCurrentSite().getId(), shareUser);
				}
				context.put("isSaferUser", isSaferUser);
				String html = FreeMarkerTemplateUtils.processTemplateIntoString(freeMarkerConfiguration.getTemplate(templateName,"UTF-8"),context);

				emailManager.sendNotificationEmailAsync( email, subject, null, html, new String[] { filePath }, new String[] { fileName } ,
						new CallBack() {

							@Override
							public void onSuccess() {
							}

							@Override
							public void onError(Throwable e) {
								e.printStackTrace();
							}
						} );
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void clearSharedEvents(List<Integer> sharedEventIds) {
		eventManager.clearSharedEvents(sharedEventIds, getCurrentUser());
	}
	
//	private  String getERGPath() throws IOException{
//		String s = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();
//		if(s.contains(":")){
//			s = s.substring(1);
//		}
//		s = s.replace("com/safer/one/gwt/server/SaferOneServiceImpl.class", "");
//		return s+"static/ergguide/Pdf/";
//	}
	
	public void fillTemplate(String base64ScreenShot, String base64MetWidgetdata, String base64SemaphoreData, String base64PlumeVerticalProfile, MetAverageDTO average, LightningInfoDTO lightning, List<Pair<String, String>> lightningInfoMap, ImpactedPoisCalculatedDTO data,
			String impactedSorting, SiteSettings siteSettings,String templateName,String outFileName, Flags f, ScenarioRun sr, ScenarioRunDTO scenarioRunDTO) {
		final String inFilePath = getBaseFileFolderForSiteFiles()+templateName;
		
		InputStream templateFileInputStream = null;
		
		try {
			Site site = getCurrentSite();
			final Integer mapWidthI = siteSettings.getMapWidth();
			final float mapWidth = (mapWidthI==null?Config.DEFAULT_MAP_IMAGE_WIDTH:mapWidthI.floatValue());
			if(Files.isRegularFile(Paths.get(inFilePath))){
				templateFileInputStream = new FileInputStream(inFilePath);
			}else{
				if(data.isErg()) {
					templateFileInputStream = ergTemplateResource.getInputStream();
				}else if(data.isScenario()) {
					templateFileInputStream = scenarioTemplateResource.getInputStream();
				}else {
					templateFileInputStream = quickTemplateResource.getInputStream();
				}
			}
			
			OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(new File(outFileName)));
			InputStream in = new BufferedInputStream(templateFileInputStream);
			
			IXDocReport report = XDocReportRegistry.getRegistry().loadReport(in, TemplateEngineKind.Freemarker);
			FreemarkerTemplateEngine engine = ((FreemarkerTemplateEngine)report.getTemplateEngine());
			engine.getFreemarkerConfiguration().setTemplateExceptionHandler(new IgnoreNullsTemplateExceptionHandler());
			
			FieldsMetadata metadata = report.createFieldsMetadata();
			metadata.addFieldAsImage("MetImage");
			metadata.addFieldAsImage("MapImage");
			metadata.addFieldAsImage("SemaphoreImage");
			metadata.addFieldAsImage("PlumeVerticalProfileImage");
			
			metadata.addFieldAsList("alarms.name");
			metadata.addFieldAsList("alarms.type");
			metadata.addFieldAsList("alarms.startDate");
			metadata.addFieldAsList("alarms.lastValue");
			metadata.addFieldAsList("alarms.activityFormatted");
			metadata.addFieldAsList("alarms.maxValueFormatted");
			metadata.addFieldAsList("alarms.acknowledgedAt");
			
			metadata.addFieldAsList("pois.name");
			metadata.addFieldAsList("pois.typeName");
			metadata.addFieldAsList("pois.distance");
			metadata.addFieldAsList("pois.address");
			metadata.addFieldAsList("pois.phone");
			metadata.addFieldAsList("pois.firstImpactMinute");
			metadata.addFieldAsList("pois.firstImpactDuration");
			metadata.addFieldAsList("pois.outdoorPeakConc");
			
			metadata.addFieldAsList("pois.location");
			metadata.addFieldAsList("pois.impactType");
			metadata.addFieldAsList("pois.uiImpactType");
			
			metadata.addFieldAsList("mets.metStation.name");
			metadata.addFieldAsList("mets.metData.maxWindSpeed");
			metadata.addFieldAsList("mets.metData.windSpeed");
			metadata.addFieldAsList("mets.metData.windDirection");
			metadata.addFieldAsList("mets.metData.feelsLike");
			metadata.addFieldAsList("mets.metData.temperature");
			metadata.addFieldAsList("mets.metData.hStability");
			metadata.addFieldAsList("mets.metData.vStability");
			
			report.setFieldsMetadata(metadata);
			
			IContext context = report.createContext();
			
			if(base64MetWidgetdata!=null && base64MetWidgetdata.length()>0){
				IImageProvider image = new ByteArrayImageProvider(Base64.getDecoder().decode( base64MetWidgetdata.substring(base64MetWidgetdata.indexOf(',')+1)), true);
				image.setUseImageSize(true);
				image.setResize(true);
				context.put("MetImage", image);
			}
			
			if(base64ScreenShot!=null && base64ScreenShot.length()>0){
				IImageProvider image1 = new ByteArrayImageProvider(Base64.getDecoder().decode( base64ScreenShot.substring(base64ScreenShot.indexOf(',')+1)));
				image1.setUseImageSize(true);
				image1.setResize(true);
				image1.setWidth(mapWidth);
				context.put("MapImage", image1);
			}
			
			if(base64PlumeVerticalProfile!=null && base64PlumeVerticalProfile.length()>0){
				String imageData =  base64PlumeVerticalProfile.substring(base64PlumeVerticalProfile.indexOf(',')+1);
				if(!imageData.isEmpty()) {
					IImageProvider image2 = new ByteArrayImageProvider(Base64.getDecoder().decode(imageData));
					image2.setUseImageSize(false);
					image2.setResize(true);
					context.put("PlumeVerticalProfileImage", image2);
				}
			}
			
			if(base64SemaphoreData!=null && base64SemaphoreData.length()>0){
				String imageData =  base64SemaphoreData.substring(base64SemaphoreData.indexOf(',')+1);
				if(!imageData.isEmpty()) {
					IImageProvider image2 = new ByteArrayImageProvider(Base64.getDecoder().decode(imageData));
					image2.setUseImageSize(false);
					image2.setResize(true);
					context.put("SemaphoreImage", image2);
				}
			}
			
			MetAverageDTO metAverageDTO = null;
			HashMap<String, String> metAverageDTOLabels = null;
			if(f.includeMet){
				metAverageDTO = average;			
				metAverageDTOLabels = new HashMap<String, String>();
				if(metAverageDTO!=null) {
					unitsManager.toCustom(metAverageDTO.getMetData(), getCurrentSite().getId(),getCurrentUser().getId(),metAverageDTOLabels);
					Double windDir = metAverageDTO.getWindDirection();
					if(windDir!=null){
						metAverageDTO.getMetData().setWindDirectionText(FormatUtils.getWindDirection(windDir));
					}
				}
			}
			//alarms
			Units units = unitsManager.getUnits(site.getId(),getCurrentUser().getId());

			List<AlarmDTO> allAlarmsDTO = new ArrayList<AlarmDTO>();
			if(f.includeAlarms){
				List<Alarm> allAlarms = alarmManager.getActiveAlarms(site);
				allAlarmsDTO = mapper.mapAsList(allAlarms,  AlarmDTO.class);
				for(AlarmDTO alarm : allAlarmsDTO) {
					alarm.setStartDate(DateUtils.convertToTimezone(alarm.getStartDate(), site.getTimeZone()));
					alarm.setLastDate(DateUtils.convertToTimezone(alarm.getLastDate(), site.getTimeZone()));
					alarm.setAcknowledgedAt(DateUtils.convertToTimezone(alarm.getAcknowledgedAt(), site.getTimeZone()));
					alarm.setMaxValueFormatted(FormatUtils.formatAlarmValue(alarm.getMaxValue(), alarm.getType(),alarm.getUnitLabel(), units));
				}
			}
		
			ErgEventDTO ergEvent = data.getErgEvent();
			ErgResults ergResults = data.getErgResults();
			//erg
			if(ergEvent!=null) {
				Map<String,Object> ergTemplateFields = fillTemplateErgFields(data, units, ergResults);
				context.putMap(ergTemplateFields);

				List<MetAverageDTO> averagesDto = new ArrayList<MetAverageDTO>();
				Date relTime = ergEvent.getReleaseTime();
				List<MetAverage> averages = (relTime==null?null:dataSourceManager.getAllMetAverages(site, DateUtils.convertFromTimezoneToUtc(relTime,site.getTimeZone()), MetAverageType.ONE_MINUTE_AVERAGE));
				
				if(averages!=null){	
					for(MetAverage avg : averages) {
						MetAverageDTO averageDto = mapper.map(avg, MetAverageDTO.class);
						unitsManager.toCustom(averageDto.getMetData(), getCurrentSite().getId(),getCurrentUser().getId());
						averagesDto.add(averageDto);
					}
				}
				context.put("mets", averagesDto);
			}
			

			if(data.getScenarioId()!=null) {
				context.put("scenarioOutput", scenarioRunDTO.getScenarioOut());
				context.put("scenarioInput", scenarioRunDTO);
				context.put("scenarioRun", sr);
				String scenarioName = scenarioRunDTO.getName().replace("-", "­"); //convert minus to non-breaking-dash (alt 0173) so that the date will not be word-wrapped 
				context.put("scenarioName", scenarioName);
				
				LinkedHashMap<String,String> scenarioData = new LinkedHashMap<>();
				if(scenarioRunDTO.getScenarioType() == ScenarioType.SAL) {
					scenarioData.put("Event Name:",  scenarioName);
				}
				scenarioData.putAll(ScenarioUtils.getScenarioData(scenarioRunDTO, units));
				if(scenarioRunDTO.getScenarioType() == ScenarioType.SAL) {
					String location = scenarioData.get(ScenarioUtils.RELEASE_LOCATION_LABEL);
					scenarioData.remove(ScenarioUtils.RELEASE_LOCATION_LABEL);
					scenarioData.put("Estimated Release Location:", location);
					scenarioData.put("Address:",  getAddressFromPosition(scenarioRunDTO.getScenarioOut().getSalOutDto().getReleaseLocation()));
				}
				if(scenarioRunDTO.getScenarioType() == ScenarioType.ABC) {
					scenarioData.remove(ScenarioUtils.MAX_RELEASE_RATE_LABEL);
				}
				context.put("scenarioData", scenarioData.entrySet());

				if(scenarioRunDTO.getScenarioType() == ScenarioType.SAL || scenarioRunDTO.getScenarioType() == ScenarioType.ABC) {
					Collections.sort(sr.getSensors(), new Comparator<SensorModeling>() {
	
						@Override
						public int compare(SensorModeling o1, SensorModeling o2) {
							return o1.getId().compareTo(o2.getId());
						}
					});
					for(SensorModeling s : sr.getSensors()) {
						Collections.sort(s.getAverages(), new Comparator<SensorAverageModeling>() {
							
							@Override
							public int compare(SensorAverageModeling o1, SensorAverageModeling o2) {
								return o1.getDateTaken().compareTo(o2.getDateTaken());
							}
						});
					}
				}

				if(scenarioRunDTO.getScenarioType() == ScenarioType.ABC) {
					LinkedHashMap<String,String> scenarioReleaseEstimate = new LinkedHashMap<>();
					final SimpleDateFormat timeSdf = new SimpleDateFormat("HH:mm");

					for(int i=1;i<=120;i++) {
						if(scenarioRunDTO.getScenarioOut().getAbcReleaseRate()!=null && scenarioRunDTO.getScenarioOut().getAbcReleaseRate().containsKey(i)) {
							Double value = scenarioRunDTO.getScenarioOut().getAbcReleaseRate().get(i);
							String valueCustom = units.convertAndFormat(UnitParam.IDP_OUTPUT_MASSRATE,value);
							Date key = DateUtils.addMinutes(DateUtils.convertToTimezone(sr.getReleaseTime(), site.getTimeZone()),i-1);
							scenarioReleaseEstimate.put(timeSdf.format(key) + " ("+ i+" min)" , valueCustom);
						}
					}
					context.put("scenarioReleaseEstimate",  scenarioReleaseEstimate.entrySet());					
				}
				

				
				if(sr.getScenarioType() == ScenarioType.SAL) {
					context.put("scenarioReleaseTime", DateUtils.convertToTimezone(sr.getReleaseTime(), site.getTimeZone()));
				}else {
					context.put("scenarioReleaseTimeFiveMinutes", DateUtils.convertToTimezone(DateUtils.roundToNextFiveMinutes(sr.getReleaseTime()), site.getTimeZone()));
					context.put("scenarioReleaseTime", DateUtils.convertToTimezone(sr.getReleaseTime(), site.getTimeZone()));
				}			
				

				
				if(sr.getRealTimeMetData()!=null && sr.getCommonMetId()!=null) {
					Iterator<MetStationModelling> it = sr.getRealTimeMetData().iterator();
					while(it.hasNext()) {
						MetStationModelling met = it.next();
						if(met.getId().equals(sr.getCommonMetId())) {
							context.put("scenarioCommonMet", met);
							break;
						}
					}
				}
				
				
				ScenarioOutDTO scenarioOut = sr.getScenarioOutputDTO();
				
				Map<String,String> scenarioSourceCharacteristics = new LinkedHashMap<String, String>();
				scenarioSourceCharacteristics.put("Occurrence of flash:", scenarioOut.isFlash()?"Yes":"No");
				scenarioSourceCharacteristics.put("Pool formation:", scenarioOut.isPoolFormation()?"Yes":"No");
				if(sr.getScenarioOutputDTO().isPoolFormation()) {
					scenarioSourceCharacteristics.put("Maximum pool area:", units.convertAndFormatWithUM(UnitParam.IDP_SCEN_POOLAREA,scenarioOut.getMaxPoolArea()));
					if(scenarioOut.getMaxEvaporationRate()!=null) {
						scenarioSourceCharacteristics.put("Maximum evaporation rate", units.convertAndFormatWithUM(UnitParam.IDP_OUTPUT_MASSRATE,scenarioOut.getMaxEvaporationRate()));
					}
					if(scenarioOut.getAverageEvaporationRate()!=null) {
						scenarioSourceCharacteristics.put("Average pool evaporation rate", units.convertAndFormatWithUM(UnitParam.IDP_OUTPUT_MASSRATE,scenarioOut.getAverageEvaporationRate()));
					}
					if(scenarioOut.getEvaporationTime()!=null && scenarioOut.getEvaporationTime()!=0) {
						scenarioSourceCharacteristics.put("Total pool evaporation time",  units.convertAndFormatWithUM(UnitParam.IDP_OUTPUT_TIME,scenarioOut.getEvaporationTime()));
					}
				}
				if(scenarioOut.getReleaseDuration()!=null && scenarioOut.getReleaseDuration()!=0) {
					scenarioSourceCharacteristics.put("Release duration", units.convertAndFormatWithUM(UnitParam.IDP_OUTPUT_TIME,scenarioOut.getReleaseDuration()));	
				}
				if(scenarioOut.getMaxReleaseRate()!=null) {
					if(ScenarioType.TANK_RELEASE.equals(sr.getScenarioType()) || ScenarioType.PIPE_RELEASE.equals(sr.getScenarioType()) || ScenarioType.PUDDLE_RELEASE.equals(sr.getScenarioType())) {
						scenarioSourceCharacteristics.put("Maximum release rate", units.convertAndFormatWithUM(UnitParam.IDP_OUTPUT_MASSRATE,scenarioOut.getMaxReleaseRate()));
					}else if(ScenarioType.STACK_RELEASE.equals(sr.getScenarioType())) {
						scenarioSourceCharacteristics.put("Maximum release rate", units.convertAndFormatWithUM(UnitParam.IDP_SCEN_STACKRATE,scenarioOut.getMaxReleaseRate()));
					}else {
						scenarioSourceCharacteristics.put("Maximum release rate", units.convertAndFormatWithUM(UnitParam.IDP_SCEN_MASSRATE,scenarioOut.getMaxReleaseRate()));	
					}
						
				}
				context.put("scenarioSourceCharacteristics", scenarioSourceCharacteristics.entrySet());
				
				if(scenarioOut.isHasPoolTwoHours()) {
					Map<String,String> scenarioPoolContents = new LinkedHashMap<String, String>();				
					scenarioPoolContents.put("Mass", units.convertAndFormatWithUM(UnitParam.IDP_OUTPUT_MASSAMOUNT,scenarioOut.getPoolMass()));
					scenarioPoolContents.put("Volume", units.convertAndFormatWithUM(UnitParam.IDP_OUTPUT_VOLUME,scenarioOut.getPoolVolume()));
					scenarioPoolContents.put("Temperature", units.convertAndFormatWithUM(UnitParam.IDP_OUTPUT_TEMPERATURE,scenarioOut.getPoolTemperature()));
					scenarioPoolContents.put("Diameter", units.convertAndFormatWithUM(UnitParam.IDP_SCEN_POOLDIA,scenarioOut.getPoolDiameter()));
					scenarioPoolContents.put("Depth", units.convertAndFormatWithUM(UnitParam.IDP_SCEN_POOLDEPTH,scenarioOut.getPoolDepth()));
					context.put("scenarioPoolContents", scenarioPoolContents.entrySet());
				}
				
				if(scenarioOut.isHasTankTwoHours()) {
					Map<String,String> scenarioTankContents = new LinkedHashMap<String, String>();
					scenarioTankContents.put("Mass", units.convertAndFormatWithUM(UnitParam.IDP_OUTPUT_MASSAMOUNT,scenarioOut.getTankMass()));
					scenarioTankContents.put("Pressure", units.convertAndFormatWithUM(UnitParam.IDP_OUTPUT_PRESSURE,scenarioOut.getTankPressure()));
					scenarioTankContents.put("Temperature", units.convertAndFormatWithUM(UnitParam.IDP_OUTPUT_TEMPERATURE,scenarioOut.getTankTemperature()));
					
					double remainingheight = 0;
					Double totalHeight = sr.getScenario().getEmissionSource().getTankHeight();
					if(totalHeight!=null && totalHeight!=0 ) {
						remainingheight = scenarioOut.getTankLiquidLevel()*100 / totalHeight;
					}
					scenarioTankContents.put("Liquid Level", units.convertAndFormatWithUM(UnitParam.IDP_SCEN_LIQLEVEL,scenarioOut.getTankLiquidLevel()) + " (" + units.formatWithUM(UnitDesc.fraction_percent,remainingheight) + ")");
					
					context.put("scenarioTankContents", scenarioTankContents.entrySet());
				}
			
				ChemicalDetailsDTO chem = sr.getScenarioOutputDTO().getChemical();
				context.put("chemical", chem);
				boolean hasCompositionEvaporatingStream = sr.getScenarioOutputDTO().isPoolFormation() && sr.getScenarioOutputDTO().getCompositionEvaporatingStream()!=null && sr.getScenarioOutputDTO().getCompositionEvaporatingStream().getCategory().isMixture();
				context.put("hasCompositionEvaporatingStream", hasCompositionEvaporatingStream);
				context.put("noHazmatInfo",  chem.getHazmatInfo()==null || chem.getHazmatInfo().equalsIgnoreCase("<br />") || chem.getHazmatInfo().trim().isEmpty());
				
				Pattern p = Pattern.compile("[^\\u0009\\u000A\\u000D\\u0020-\\uD7FF\\uE000-\\uFFFD\\u10000-\\u10FFF]+"); 

				if(chem.getHazmatInfo()!=null) {
					String hazmatInfo = chem.getHazmatInfo().replace("<br>", "<br />");
					hazmatInfo = p.matcher(hazmatInfo).replaceAll(" ");
					context.put("msdsHazmatInfo", hazmatInfo);
					metadata.addFieldAsTextStyling("msdsHazmatInfo", SyntaxKind.Html, true);
				}
				if(chem.getNotes()!=null) {
					String notes = chem.getNotes().replace("<br>", "<br />");
					notes = p.matcher(notes).replaceAll(" ");
					context.put("chemicalNotes", notes);
					metadata.addFieldAsTextStyling("chemicalNotes", SyntaxKind.Html, true);
				}	
				
				for(SaferPointOfInterest poi : data.getImpactedPois()) {
					String notes = poi.getNotes().replace("<br>", "<br />");
					notes = p.matcher(notes).replaceAll(" ");
					poi.setNotes(notes);
				}
				
				metadata.addFieldAsTextStyling("poi.notes", SyntaxKind.Html, true);
				
			}
			
			if(lightning!=null && lightningInfoMap!=null) {
				context.put("lightningData", lightningInfoMap);
				context.put("lightning", lightning);
			}
			context.put("met", metAverageDTO);
			context.put("metUnit", metAverageDTOLabels);
			context.put("alarms", allAlarmsDTO);
			context.put("pois", data.getImpactedPois());
			context.put("downwind", data.isDownwind());
			context.put("erg", ergEvent);
			context.put("units", units);
			context.put("site", getCurrentSite());
			context.put("siteSettings", getSiteSettings(null));
			context.put("user", getCurrentUser());
			context.put("dateTime",DateUtils.convertToTimezone(new Date(), getCurrentSite().getTimeZone()));
			context.put("locationLatLng", data.getCenter());
			context.put("locationAddress",  getAddressFromPosition(data.getCenter()));
			context.put("impactedSorting", impactedSorting);
			context.put("f", f);
			
			BeansWrapper wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();
			TemplateHashModel staticModels = wrapper.getStaticModels();
			
			TemplateHashModel formatUtils = (TemplateHashModel) staticModels.get("com.safer.one.gwt.shared.FormatUtils");  
			context.put("FormatUtils", formatUtils);
			
			TemplateHashModel scenarioUtils = (TemplateHashModel) staticModels.get("com.safer.one.gwt.shared.ScenarioUtils");  
			context.put("ScenarioUtils", scenarioUtils);			
			
			TemplateHashModel dateUtils = (TemplateHashModel) staticModels.get("com.util.DateUtils");  
			context.put("DateUtils", dateUtils);
			 
			TemplateHashModel enumModels = wrapper.getEnumModels();
			
			TemplateHashModel unitParamEnums = (TemplateHashModel) enumModels.get("com.safer.one.gwt.shared.enums.UnitParam"); 
			context.put("UnitParam", unitParamEnums);
			
			TemplateHashModel unitDescEnums = (TemplateHashModel) enumModels.get("com.safer.one.gwt.shared.enums.UnitDesc"); 
			context.put("UnitDesc", unitDescEnums);
			
			TemplateHashModel scenarioTypeEnums = (TemplateHashModel) enumModels.get("com.safer.one.gwt.shared.enums.ScenarioType"); 
			context.put("ScenarioType", scenarioTypeEnums);
			
			TemplateHashModel sensorTypeEnums = (TemplateHashModel) enumModels.get("com.safer.one.gwt.shared.enums.SensorType"); 
			context.put("SensorType", sensorTypeEnums);
			
			addFlags(context,f);
			
			report.process(context, fileOut);
			fileOut.flush();
			fileOut.close();
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	private void addFlags(IContext context, Flags f) {
		context.put("includeAlarmHistory", f.includeAlarmHistory);
		context.put("includeAlarms", f.includeAlarms);
		context.put("includeCorridorDetails", f.includeCorridorDetails);
		context.put("includeCSVImpactedPlaces", f.includeCSVImpactedPlaces);
		context.put("includeErgGuide", f.includeErgGuide);
		context.put("includeKmz", f.includeKmz);
		context.put("includeMet", f.includeMet);
		context.put("includeMetHistory", f.includeMetHistory);
		context.put("includeMap", f.includeMap);
		context.put("includeScenarioInputs", f.includeScenarioInputs);
		context.put("includeOutputSummary", f.includeOutputSummary);
		context.put("includeVerticalProfile", f.includeVerticalProfile);
		context.put("includeReceptorImpactSummary", f.includeReceptorImpactSummary);
		context.put("includeReceptorImpactDetails", f.includeReceptorImpactDetails);
		context.put("includeMsdsHazmatInfo", f.includeMsdsHazmatInfo);
		context.put("includeChemicalProperties", f.includeChemicalProperties);
		context.put("includeSensors", f.includeSensors);
	}

	private String getAddressFromPosition(PointDTO position) {
		String address = null;
		GeoApiContext geoApi = new GeoApiContext().setApiKey("AIzaSyCzmBr4pyiaRuG_5UkXdJtp7lQGmUIwY40");
		try {
			GeocodingResult[] results =  GeocodingApi.reverseGeocode(geoApi, new LatLng(position.getLatitude(), position.getLongitude())).await();
			if(results.length != 0) {
				address = results[0].formattedAddress;
			}
		} catch (Exception e) { 
			e.printStackTrace();
		}
		return address;
	}
	
	private Map<String,Object> fillTemplateErgFields(ImpactedPoisCalculatedDTO data, Units units, ErgResults ergResults) {
		Map<String,Object> context = new HashMap<String, Object>();
		if(ergResults!=null) {
			String iid="", pad="See Guide", oed="",bed="", explMass="",explVolume="",fd="",sd="";
			String ergDiameter="", ergLength="", ergMass="", ergMinTimeToFailure="",ergAproxTimeToEmpty="",ergCoolingFlowRate=""
					,ergFireballRadius="", ergEmergencyEvDistance="",ergMinEvDistance="",ergPreferredDistance="", ergCapacity="";
			String shelterInPlace = "";
			UnitDesc unit = units.getUnitDesc(UnitParam.IDP_ERG_UNIT);
			switch (unit) {
			case metric:
				iid = units.formatWithUM(UnitDesc.m, ergResults.getIsoM());
				if (ergResults.getPadM() != 0) {
					pad = units.formatWithUM(UnitDesc.km, ergResults.getPadM());
				}
				bed = units.formatWithUM(UnitDesc.m, ergResults.getBedM());
				oed = units.formatWithUM(UnitDesc.m, ergResults.getOedM());
				shelterInPlace = units.formatWithUM(UnitDesc.m, ergResults.getShelterM1()) + " - " + units.formatWithUM(UnitDesc.m, ergResults.getShelterM2());
				explMass = units.formatWithUM(UnitDesc.kg, ergResults.getMassMM());
				explVolume =  units.formatWithUM(UnitDesc.liter, ergResults.getMassVM());
				fd =  units.formatWithUM(UnitDesc.m, ergResults.getFrdM());
				sd =  units.formatWithUM(UnitDesc.m, ergResults.getSdM());
				ergDiameter = units.formatWithUM(UnitDesc.m, ergResults.getDiameterM());
				ergDiameter = (ergDiameter.startsWith(".")?"0"+ergDiameter:ergDiameter);
				
				ergLength = units.formatWithUM(UnitDesc.m, ergResults.getLengthM());
				ergMass = units.formatWithUM(UnitDesc.kg, ergResults.getMassM());
				ergMinTimeToFailure = units.formatWithUM(UnitDesc.min, ergResults.getMinTimeToFailure());
				ergAproxTimeToEmpty = units.formatWithUM(UnitDesc.min, ergResults.getAproxTimeToEmpty());
				ergFireballRadius = units.formatWithUM(UnitDesc.m, ergResults.getFireballRadiusM());
				ergEmergencyEvDistance = units.formatWithUM(UnitDesc.m, ergResults.getEmergencyEvDistanceM());
				ergMinEvDistance = units.formatWithUM(UnitDesc.m, ergResults.getMinEvDistanceM());
				ergPreferredDistance = units.formatWithUM(UnitDesc.m, ergResults.getPreferredDistanceM());
				ergCoolingFlowRate = units.formatWithUM(UnitDesc.liter$min, ergResults.getCoolingFlowRateM());
				ergCapacity = units.formatWithUM(UnitDesc.liter,ergResults.getCapacityM());
				break;
			case imperial:
				iid = units.formatWithUM(UnitDesc.ft, ergResults.getIsoE());
				if (ergResults.getPadE() != 0) {
					pad = units.formatWithUM(UnitDesc.mile, ergResults.getPadE());
				}
				bed = units.formatWithUM(UnitDesc.ft, ergResults.getBedE());
				oed = units.formatWithUM(UnitDesc.ft, ergResults.getOedE());
				shelterInPlace = units.formatWithUM(UnitDesc.ft, ergResults.getShelterE1()) + " - " + units.formatWithUM(UnitDesc.ft, ergResults.getShelterE2());
				explMass = units.formatWithUM(UnitDesc.lb, ergResults.getMassME());
				explVolume =  units.formatWithUM(UnitDesc.USgallon, ergResults.getMassVE());
				fd =  units.formatWithUM(UnitDesc.ft, ergResults.getFrdE());
				sd =  units.formatWithUM(UnitDesc.ft, ergResults.getSdE());
				ergDiameter = units.formatWithUM(UnitDesc.ft, ergResults.getDiameterE());
				ergDiameter = (ergDiameter.startsWith(".")?"0"+ergDiameter:ergDiameter);
				
				ergLength = units.formatWithUM(UnitDesc.ft, ergResults.getLengthE());
				ergMass = units.formatWithUM(UnitDesc.lb, ergResults.getMassE());
				ergMinTimeToFailure = units.formatWithUM(UnitDesc.min, ergResults.getMinTimeToFailure());
				ergAproxTimeToEmpty = units.formatWithUM(UnitDesc.min, ergResults.getAproxTimeToEmpty());
				ergFireballRadius = units.formatWithUM(UnitDesc.ft, ergResults.getFireballRadiusE());
				ergEmergencyEvDistance = units.formatWithUM(UnitDesc.ft, ergResults.getEmergencyEvDistanceE());
				ergMinEvDistance = units.formatWithUM(UnitDesc.ft, ergResults.getMinEvDistanceE());
				ergPreferredDistance = units.formatWithUM(UnitDesc.ft, ergResults.getPreferredDistanceE());
				ergCoolingFlowRate = units.formatWithUM(UnitDesc.USgallons$min, ergResults.getCoolingFlowRateE());
				ergCapacity = units.formatWithUM(UnitDesc.USgallon,ergResults.getCapacityE());
				break;
			default:
				break;
			} 
			context.put("ergIid", iid);
			context.put("ergPad", pad);
			//ied
			context.put("ergOed", oed);
			context.put("ergBed", bed);
			context.put("ergShelter", shelterInPlace);
			context.put("ergFd", fd);
			context.put("ergSd", sd);
			context.put("ergExplMass", explMass);
			context.put("ergExplVolume",explVolume);
			//bleve 
			context.put("ergDiameter", ergDiameter);
			context.put("ergLength", ergLength);
			context.put("ergMass", ergMass);
			context.put("ergMinTimeToFailure", ergMinTimeToFailure);
			context.put("ergAproxTimeToEmpty", ergAproxTimeToEmpty);
			context.put("ergFireballRadius",ergFireballRadius);
			context.put("ergEmergencyEvDistance", ergEmergencyEvDistance);
			context.put("ergMinEvDistance", ergMinEvDistance);
			context.put("ergPreferredDistance",ergPreferredDistance);
			context.put("ergCoolingFlowRate", ergCoolingFlowRate);
			context.put("ergCapacity", ergCapacity);
			
			context.put("ergIsCommonVisible", data.isErgIsCommonVisible());
			context.put("ergIsSpillTimeVisible", data.isErgIsSpillTimeVisible());
			
			LinkedHashMap<String,String> ergChemicalData = new LinkedHashMap<>();
			ErgEventDTO erg = data.getErgEvent();
			if(erg!=null && erg.getErgType() == ErgType.HAZMAT && erg.getErgChemicalId()!=null && StringUtils.isNotEmpty(erg.getErgChemicalId().getErgChemicalName())) {
				ergChemicalData.put("Chemical: ", erg.getErgChemicalId().getErgChemicalName());
				if(!data.isErgIsCommonVisible() || (data.isErgIsCommonVisible() && !erg.getCommon())) {
					ergChemicalData.put("Spill Size: ", erg.getSpillSize().toString());
					if(data.isErgIsSpillTimeVisible()) {
						ergChemicalData.put("Spill Time: ", erg.getSpillTime().toString());	
					}
					if(StringUtils.isNotEmpty(erg.getReleasedGas())) {
						ergChemicalData.put("Released Gas: ", erg.getReleasedGas());
					}
				}else {
					if(erg.getCommon()) {
						ergChemicalData.put("Spill Size: ", "LARGE");	
					}
					if(data.isErgIsSpillTimeVisible()) {
						ergChemicalData.put("Spill Time: ", erg.getSpillTime().toString());	
					}
					ergChemicalData.put("Container: ", erg.getHazmatContainer().toString());
				}
				ergChemicalData.put("Isolate (IID): ", iid);
				ergChemicalData.put("Protect (PAD): ", pad);
			}
			context.put("ergChemicalData", ergChemicalData.entrySet());
		}
		return context;
	}
	
	
	

	private String getBaseFileFolderForSiteFiles(){
		return ServerUtils.getBaseFileFolderForSiteFiles(true,getCurrentSite().getId(),properties);
	}

	private String getBaseFileFolderForUserFiles(String downloadType){
		return ServerUtils.getBaseFileFolderForUserFiles(downloadType,true,getCurrentUser().getId(), properties);
	}

	/**
	 * gets the exact type average, or, when type is 5 min and there is no 5 min type average at provided date, gets a partial average using 1 min averages
	 * @param date
	 * @param metStationId
	 * @param type - 
	 * @return
	 */
	@SuppressWarnings("deprecation")
	@Override
	public MetAverageDTO getMetAverage(Date date, int metStationId, MetAverageType type) {
		date = DateUtils.convertFromTimezoneToUtc(date, getCurrentSite().getTimeZone());
		MetStation metStation = dataSourceManager.getMetStation(metStationId);
		MetAverage avg = dataSourceManager.getMetAverage(date, metStation, type);
		if (avg == null && type == MetAverageType.FIVE_MINUTE_AVERAGE && date.getMinutes()%5 != 0) {
			Date start = new Date(date.getTime());
			start.setMinutes(start.getMinutes()-start.getMinutes()%5+1);
			avg = getAverageOfAverage(dataSourceManager.getMetAverages(metStation, MetAverageType.ONE_MINUTE_AVERAGE, start, date));
		}
		if (avg == null) {
			return null;
		} else {
			avg.setDateTaken(DateUtils.convertToTimezone(avg.getDateTaken(), getCurrentSite().getTimeZone()));
			return mapper.map(avg, MetAverageDTO.class);
		}
	}

	@Override
	public void clearManualMetData() {
		ManualMetData manualMet = dataSourceManager.getManualMetData(getCurrentUser(), getCurrentSite());
		if(manualMet!=null) {
			manualMet.setHidden(true);
			dataSourceManager.save(manualMet);
		}
	}

	@Override
	public int getUserCount(int organizationId) {
		Organization organization = new Organization();
		organization.setId(organizationId);
		return userManager.getUserCount(organization).intValue();
	}

	@Override
	public Long getSitesCount(Integer organizationId) {
		return siteManager.getSitesCount(organizationId);
	}

	@Override
	public boolean[] areQuickAndERGTemplatesAvailableToDownload(Integer siteId) {
		final boolean falseRet[] = {false,false,false};
		if(!getCurrentUser().isSaferAdmin()){
			return falseRet;
		}
		Site site = siteManager.getSite(siteId);
		if(site==null){
			return falseRet;
		}
		
		final String baseFolderPath = ServerUtils.getBaseFileFolderForSiteFiles(true,site.getId(),properties);
		//ServerUtils.getBaseFileFolder(getCurrentUser(),"template", false, properties);

		boolean quickTemplateExists = Files.isRegularFile(Paths.get(baseFolderPath+Config.QUICK_TEMPLATE));
		boolean ergTemplateExists = Files.isRegularFile(Paths.get(baseFolderPath+Config.ERG_TEMPLATE));
		boolean scenarioTemplateExists = Files.isRegularFile(Paths.get(baseFolderPath+Config.SCENARIO_TEMPLATE));
	
		if(!quickTemplateExists){
			try {
				FileUtils.copyFile(quickTemplateResource.getFile(), new File(baseFolderPath+Config.QUICK_TEMPLATE));
				quickTemplateExists = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(!ergTemplateExists){
			try {
				FileUtils.copyFile(ergTemplateResource.getFile(), new File(baseFolderPath+Config.ERG_TEMPLATE));
				ergTemplateExists = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(!scenarioTemplateExists){
			try {
				FileUtils.copyFile(scenarioTemplateResource.getFile(), new File(baseFolderPath+Config.SCENARIO_TEMPLATE));
				scenarioTemplateExists = true;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		final boolean []ret ={quickTemplateExists , ergTemplateExists,scenarioTemplateExists};
		return ret;
	}

	@Override
	public Date getEarthNetworkLastActivityDate(String username, String password, String providerId, String stationId) {
		username = getEarthNetworkKey();
		MetReading metReading = earthNetworkApiClient.getMetReading(username, providerId, stationId);
		if(metReading==null) {
			return null;
		}
		Date latest = metReading.getDateTaken();
		return DateUtils.convertToTimezone(latest, getCurrentSite().getTimeZone());
	}
	
	
	@Override
	public int getUserCountForSite(int siteId) {
		return siteManager.getUserCount(siteId);
	}

	@Override
	public ZoneDTO saveSiteBorder(Integer zoneId, List<PointDTO> path, String name, String color, boolean impactable, Integer groupId) {
		name= HtmlUtils.escapeHtml(name);
		color= HtmlUtils.escapeHtml(color);

		if(path==null||path.size()<2){
			throw new RuntimeException("Drawing must contain at least 2 points");
		}
		Zone zone = zoneManager.save(zoneId, getCurrentSite(),path, name, color, impactable, groupId);
		return mapper.map(zone, ZoneDTO.class);
	}

	@Override
	public void deleteSiteBorder(int id) {
		Zone zone = zoneManager.getZone(id);
		zone.setHidden(true);
		zoneManager.save(zone);
	}
	
	@Override
	public Map<Integer,ZoneDTO> getZones(Integer eventId) {
		List<Zone> siteZones =  zoneManager.getZones(getCurrentSite(),null);
		List<ZoneDTO> zones= mapper.mapAsList(siteZones, ZoneDTO.class);
		if(eventId!=null) {
			List<Zone> eventZones =  zoneManager.getZones(getCurrentSite(),eventId);
			zones.addAll(mapper.mapAsList(eventZones, ZoneDTO.class));
		}
		
		Map<Integer, ZoneDTO> zonesMap = new HashMap<Integer, ZoneDTO>(); 
		for(ZoneDTO zone : zones) {
			zonesMap.put(zone.getId(), zone);
		}
		return zonesMap;
	}
	
	@Override 
	public SettingDTO saveMapLayers(List<MapLayerType> layers) {
		User user = getCurrentUser();
		Setting setting = settingManager.saveListSettings(SettingParam.MAP_LAYERS, layers, user, null);
		return mapper.map(setting,SettingDTO.class);
	}
	
	@Override 
	public SettingDTO saveKmlLayers(List<Integer> kmlLayers) {
		User user = getCurrentUser();
		Site site = getCurrentSite();
		Setting setting = settingManager.saveListSettings(SettingParam.KML_LAYERS, kmlLayers, user, site);
		return mapper.map(setting,SettingDTO.class);
	}
	
	
	
	@Override
	public List<KmlLayerDTO> getKmlLayers() {
		Site site = getCurrentSite();
		return mapper.mapAsList(kmlLayerManager.getKmlLayers(site), KmlLayerDTO.class);
	}
	
	
	
	@Override
	public RTPointOfInterestDTO getConcentrationData(PointDTO releaseLocation, PointOfInterestDTO pointOfInterestDTO, int scenarioRunId, Double ach) {
		PointOfInterest poi;
		if(pointOfInterestDTO.getId()!=null) {
			poi = poisManager.getPoiById(pointOfInterestDTO.getId());
		}else {
			poi = mapper.map(pointOfInterestDTO, PointOfInterest.class);
		}
		RTPointOfInterestDTO rtpoi = new RTPointOfInterestDTO();
		ScenarioRun scenarioRun = scenarioManager.getScenarioRun(scenarioRunId);
		
		if(poi!=null) {
			rtpoi = mapper.map(poi, RTPointOfInterestDTO.class);
			try {
			    double keyIsopleths[]  = getScenarioKeyIsopleths(scenarioRun);
				double isopleths[] = getScenarioIsopleths(scenarioRun);
				
				final String inFilePath = ServerUtils.getBaseFileFolderForScenarioRun(scenarioRun,scenarioRun.getUser().getId(),true,properties)+Config.PLUME_DAT;
				String plumeData = FileUtils.readFileToString(new File(inFilePath));
				Plume plume = PlumeUtils.parseDispersionPlume( plumeData,  keyIsopleths==null?isopleths:keyIsopleths, releaseLocation);
				
				Double molarFraction = getMolarFraction(keyIsopleths, isopleths);
				if(ach != null) {
					rtpoi.setDefaultACH(ach);
				}
				plume.CalcInfImpact(rtpoi,  keyIsopleths==null?isopleths[0]:keyIsopleths[0], 23, molarFraction);
				
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		return rtpoi;
	}

	private Double getMolarFraction(double keyIsopleths[], double isopleths[]) {
		Double mollarFraction = null;
		if(keyIsopleths!=null&&isopleths!=null){ //TODO move this mixture adjustments inside plume.CalcInfImpact
		    if(isopleths[0]!=keyIsopleths[0]){
			mollarFraction = isopleths[0]/keyIsopleths[0];
		    }
		}
		return mollarFraction;
	}
	
	@Override
	public ChemicalDetailsDTO getChemical(int chemicalId, boolean forceFillIsoplethsForKeyComp, Boolean getMixtureComponentDetails) {
		ChemicalDetailsDTO chemical = getChemical(chemicalId, getCurrentSite(),forceFillIsoplethsForKeyComp);
		if (Boolean.TRUE.equals(getMixtureComponentDetails) && chemical.getCategory() == ChemicalType.MIXTURE) {
			Map<Integer, ChemicalDetailsDTO> mixtureDetails = new HashMap<>();
			if (chemical.getMixtureChemicals() != null) {
				for (MixtureChemicalDTO comp:chemical.getMixtureChemicals()) {
					mixtureDetails.put(comp.getCompChemical().getId(), getChemical(comp.getCompChemical().getId(), getCurrentSite(), false));
				}
			}
			chemical.setMixtureChemicalDetails(mixtureDetails);
		}
		return chemical;
	}
	
	@Override
	public ChemicalDetailsDTO getDefaultChemical(int chemicalId) {
		return getChemical(chemicalId, null, false);
	}
	
	private ChemicalDetailsDTO getChemical(int chemicalId, Site site, boolean forceFillIsoplethsForKeyComp) {
		Integer currentSiteId = getCurrentSite().getId();
		ChemicalDetailsDTO chemDetails = chemicalsManager.getChemicalDetails(chemicalId, site);
	
		List<MixtureChemicalDTO>  mixtureChemicals = chemDetails.getMixtureChemicals();
		if( forceFillIsoplethsForKeyComp &&  mixtureChemicals!=null &&  mixtureChemicals.size()>0){
		    MixtureChemicalDTO keyComponent = null;
		    for(MixtureChemicalDTO mixChem : mixtureChemicals) {
			if(mixChem.isCompKey()) {
			    keyComponent = mixChem;
			    break;
			}
		    }

		    if(keyComponent!=null){
			ChemicalDetailsDTO keyComponentDetails = chemicalsManager.getChemicalDetails(keyComponent.getCompChemical().getId(), getCurrentSite());
			if(keyComponentDetails!=null){
			    chemDetails.setConcentrationIsopleths(keyComponentDetails.getConcentrationIsopleths());
			    Iterator<ConcentrationIsoplethsDTO> it= keyComponentDetails.getConcentrationIsoplethsList().iterator();
			    while(it.hasNext()){
				ConcentrationIsoplethsDTO concIsoplethsDTO= it.next();
				if(ConcentrationIsoplethsType.FIRE_EXPLOSION.toString().equals(concIsoplethsDTO.getType())) {
					it.remove();
				}
			    }
			    chemDetails.setConcentrationIsoplethsList(keyComponentDetails.getConcentrationIsoplethsList());
			}
		    }
		}
		
		unitsManager.toCustom(chemDetails, currentSiteId,getCurrentUser().getId());
		
		for(ConcentrationIsoplethsDTO concIso : chemDetails.getConcentrationIsoplethsList()) {
			unitsManager.toCustom( concIso, currentSiteId,getCurrentUser().getId());
		}
		if(chemDetails.getParticulateIsopleths()!=null) {
			unitsManager.toCustom( chemDetails.getParticulateIsopleths(), currentSiteId,getCurrentUser().getId());
		}
		
		return chemDetails;
	}
	
	@Override	
	public Map<Date, MetDataDTO> calculateManualStability(Double windSpeed, Integer cloudinessIndex, PointDTO location, List<Date> dates) {
		Map<Date, MetDataDTO> stabilities = new HashMap<Date, MetDataDTO>();
		windSpeed = unitsManager.getUnits(getCurrentSite().getId(), getCurrentUser().getId()).toInternal(UnitParam.IDP_MET_WINDSPEED, windSpeed );
		
		for(Date date : dates) {
			Date utcDate = DateUtils.convertFromTimezoneToUtc(date, getCurrentSite().getTimeZone());
			utcDate= DateUtils.trimToMinutes(utcDate);
			Double solar = MetCalculations.getSolarManual(utcDate,location.getLatitude(), location.getLongitude(), cloudinessIndex);
			Integer stability = MetCalculations.getStabilityManual(utcDate, windSpeed, solar, location.getLatitude(), location.getLongitude(), cloudinessIndex);
			MetDataDTO data = new MetDataDTO();
			data.setSolarRadiation(solar);
			data.sethStability(stability);
			data.setvStability(stability);
			stabilities.put(date, data);
		}
		return stabilities;
	}
	
	private List<ScenarioMetDataDTO> recalculateScenarioMetData(Date releaseTime, PointDTO location, List<ScenarioMetDataDTO> scenMetData, User user, Site site){
		Date scenarioReleaseTime = DateUtils.convertFromTimezoneToUtc(DateUtils.roundToNextFiveMinutes(releaseTime), site.getTimeZone());

		ScenarioUtils.autoFillScenarioMetData(scenMetData);
		
		boolean isWizard = false;
	
		for(int i = 0; i<scenMetData.size();i++) {
			ScenarioMetDataDTO data = scenMetData.get(i);
			
			if(data.isWizard()) {
				isWizard = true;
			}
			if(data.isManual() && !data.isWizard()) {
				isWizard = false;
			}
			if(isWizard) {
				Date lineDate = DateUtils.addMinutes(scenarioReleaseTime,  data.getMinute());
				double windSpeed = unitsManager.getUnits(site.getId(),user.getId()).toInternal(UnitParam.IDP_MET_WINDSPEED, data.getMetData().getWindSpeed() );
				Double solar = MetCalculations.getSolarManual(lineDate, location.getLatitude(), location.getLongitude(), data.getCloudinessIndex());
				Integer stability = MetCalculations.getStabilityManual(lineDate, windSpeed, solar, location.getLatitude(), location.getLongitude(), data.getCloudinessIndex());
				data.getMetData().setSolarRadiation(solar);
				data.getMetData().sethStability(stability);
				data.getMetData().setvStability(stability);
			}
		}
		return scenMetData;
	}
	
	@Override
	public List<ScenarioMetDataDTO> recalculateScenarioMetData(Date releaseTime, PointDTO location, List<ScenarioMetDataDTO> scenMetData) {
		return recalculateScenarioMetData(releaseTime, location, scenMetData, getCurrentUser(), getCurrentSite());
	}
	
	@Override
	public ChemicalDetailsDTO calculateMixtureChemical(List<MixtureChemicalDTO> mixtureChemicals, MixtureModelType type, MixtureCompositionType mixtureCompositionType) {
		
		Integer siteId = getCurrentSite().getId();		
		Site site = getCurrentSite();
		MixtureChemical keyComponent = null;
		
		List<MixtureChemical> chemicals = mapper.mapAsList(mixtureChemicals, MixtureChemical.class);
		for(MixtureChemical mixChem : chemicals) {
			mixChem.setCompAmount(mixChem.getCompAmount()/100);
			mixChem.setCompChemical(chemicalsManager.findById(mixChem.getCompChemical().getId()));
			mixChem.setSite(site);
			if(mixChem.isCompKey()) {
				keyComponent = mixChem;
			}
		}
		ChemicalDetailsDTO mix = ChemicalUtils.calcMixtureProps(chemicals, mixtureCompositionType);
		
		if (keyComponent != null) {
			ChemicalDetailsDTO  keyCompDetails = chemicalsManager.getChemicalDetails(keyComponent.getCompChemical().getId(), site);
			if(keyCompDetails.getConcentrationIsoplethsList()!=null) {
				for(ConcentrationIsoplethsDTO concIso : keyCompDetails.getConcentrationIsoplethsList()) {
					if(!ConcentrationIsoplethsType.ODOR.toString().equals(concIso.getType()) && !ConcentrationIsoplethsType.FIRE_EXPLOSION.toString().equals(concIso.getType())) {
						if(keyCompDetails.getConcentrationIsopleths().equals(concIso)) {
							mix.setConcentrationIsopleths(concIso);
						}
						concIso.setId(null);
						mix.getConcentrationIsoplethsList().add(concIso);
					}
				}
			}
		}
		
		unitsManager.toCustom(mix, siteId,getCurrentUser().getId());
		for(ConcentrationIsoplethsDTO concIso : mix.getConcentrationIsoplethsList()) {
			unitsManager.toCustom( concIso, siteId,getCurrentUser().getId());
		}
		return mix;
	}

	private ChemicalDetailsDTO calculateMixtureChemicalImport(List<MixtureChemicalDTO> mixtureChemicals, MixtureModelType type, MixtureCompositionType mixtureCompositionType) {
		
		Integer siteId = getCurrentSite().getId();		
		Site site = getCurrentSite();
		MixtureChemical keyComponent = null;
		int keyCompIndex = -1;
		
		List<MixtureChemical> chemicals = mapper.mapAsList(mixtureChemicals, MixtureChemical.class);
		
		//Fetch component from db using safer no.
		for (int i = 0; i < chemicals.size(); i++) {
			MixtureChemical mixChm = chemicals.get(i);
			//use safer no. (stored as id) to locate the chemical
			mixChm.setCompChemical(chemicalsManager.findBySaferNo(mixChm.getCompChemical().getId()));
			chemicals.set(i,mixChm);
		}
		
		int index = 0;
		for(MixtureChemical mixChem : chemicals) {
			mixChem.setCompAmount(mixChem.getCompAmount()/100);			
			mixChem.setCompChemical(chemicalsManager.findById(mixChem.getCompChemical().getId()));			
			mixChem.setSite(site);
			if(mixChem.isCompKey()) {
				keyComponent = mixChem;
				keyCompIndex = index;
			}
			++index;
		}
		ChemicalDetailsDTO mix = ChemicalUtils.calcMixtureProps(chemicals, mixtureCompositionType);
		
		if (keyComponent != null) {
			ChemicalDetailsDTO  keyCompDetails = chemicalsManager.getChemicalDetails(keyComponent.getCompChemical().getId(), site);
			if(keyCompDetails.getConcentrationIsoplethsList()!=null) {
				for(ConcentrationIsoplethsDTO concIso : keyCompDetails.getConcentrationIsoplethsList()) {
					if(!ConcentrationIsoplethsType.ODOR.toString().equals(concIso.getType()) && !ConcentrationIsoplethsType.FIRE_EXPLOSION.toString().equals(concIso.getType())) {
						if(keyCompDetails.getConcentrationIsopleths().equals(concIso)) {
							mix.setConcentrationIsopleths(concIso);
						}
						concIso.setId(null);
						mix.getConcentrationIsoplethsList().add(concIso);
					}
				}
			}
		}

		//Need to convert comp amount to %
		for(MixtureChemical mixChem : chemicals) {
			mixChem.setCompAmount(mixChem.getCompAmount()*100);
		}
		
		mix.setMixtureChemicals(mapper.mapAsList(chemicals, MixtureChemicalDTO.class));			
		
		mix.setMixtureChemicalsKeyComponentIndex(keyCompIndex);
		
		return mix;
	}	
	
	
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<Integer,Integer> saveGridChangedChemicals(List<ChemicalDTO> changedChemicals) {
		if(!getCurrentUser().isSiteAdmin()){
			throw new RuntimeException("Not Allowed");
		}
		changedChemicals= (List<ChemicalDTO>)HtmlUtils.escapeHtml(changedChemicals);
	    return chemicalsManager.saveGridChangedChemicals(changedChemicals, getCurrentSite());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean saveGridChangedFeedback(List<FeedbackDTO> changedFeedback) {
		if(!getCurrentUser().isSaferAdmin()){
			throw new RuntimeException("Not Allowed");
		}
		changedFeedback= (List<FeedbackDTO>)HtmlUtils.escapeHtml(changedFeedback);
	    return feedbackManager.saveGridChangedFeedback(changedFeedback, getCurrentSite());
	}

	
	public List<VerticalProfilePointDTO> getVerticalProfileData(ScenarioRun sr) {
		
		final String inFilePath = ServerUtils.getBaseFileFolderForScenarioRun(sr, sr.getUser().getId(), true, properties)+Config.VPROFIL_DAT;
		List<VerticalProfilePointDTO> ret = new ArrayList<VerticalProfilePointDTO>();
		try {
			String vprofileData = FileUtils.readFileToString(new File(inFilePath));			
			ret = PlumeUtils.parseVerticalProfile(vprofileData);
		}catch(Exception e) {
			e.printStackTrace();
		}
		Collections.sort(ret, new Comparator<VerticalProfilePointDTO>() {

			@Override
			public int compare(VerticalProfilePointDTO o1, VerticalProfilePointDTO o2) {
				return o1.getDistance().compareTo(o2.getDistance());
			}
		});
		return ret;
	}
	
	public PlumeDTO getPlume(PointDTO location, Plume plume, int interval, boolean isSnapshot) {
		PlumeDTO plumeDto = new PlumeDTO();
		for (int level = 0; level < 3; level++) {
			try {
				List<PuffCollect> pcs;
				if (isSnapshot) {
					pcs = plume.DrawSnapshot(level, interval);
				} else {
					pcs = plume.DrawFootprint(level, 0, interval);
				}
				List<PolygonDTO> points = PlumeRenderUtils.renderPlume(location, pcs, plume.getDownDistance()[level]);
				switch (level) {
				case 0:
					plumeDto.setLow(points);
					break;
				case 1:
					plumeDto.setMedium(points);
					break;
				case 2:
					plumeDto.setHigh(points);
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return plumeDto;
	}
	
	private static double[] getScenarioIsopleths(ScenarioRun scenarioRun){
	    double isopleths[] = {scenarioRun.getScenario().getLow(),
	    				scenarioRun.getScenario().getMedium(),
	    				scenarioRun.getScenario().getHigh()};
	    return isopleths;
	}
	
	private static double[] getScenarioKeyIsopleths(ScenarioRun scenarioRun){
	    final Double keyLow = scenarioRun.getScenario().getKeyLow();
	    if(keyLow !=null){
		double isopleths[] = {keyLow,scenarioRun.getScenario().getKeyMedium(),scenarioRun.getScenario().getKeyHigh()};
		return isopleths;
	    }
	    return null;
	}
	
	private Plume readPlume( ScenarioRun scenarioRun) {
		PointDTO releaseLocation = PointDTO.fromXY(scenarioRun.getScenario().getEmissionSource().getLocation().getX(), 
				scenarioRun.getScenario().getEmissionSource().getLocation().getY());
		final String inFilePath = ServerUtils.getBaseFileFolderForScenarioRun(scenarioRun,scenarioRun.getUser().getId(),true,properties)+Config.PLUME_DAT;
		
		Plume plume = null;
		try {
			String plumeData = FileUtils.readFileToString(new File(inFilePath));
			double isopleths[]  = getScenarioKeyIsopleths(scenarioRun);
			isopleths = ((isopleths==null)?isopleths = getScenarioIsopleths(scenarioRun):isopleths);
			
			plume = PlumeUtils.parseDispersionPlume(plumeData, isopleths, releaseLocation);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return plume;
	}

	private ScenarioRunDTO getScenarioOutput(ScenarioRun scenarioRun) {
		//Integer scenarioRunId = scenarioRun.getId();
		Date utcReleaseTime = scenarioRun.getReleaseTime();

		ScenarioRunDTO scenRunDTO = (ScenarioRunDTO) getScenarioRunDTO(scenarioRun);
		scenRunDTO.setScenarioOut(scenarioRun.getScenarioOutputDTO());

		MetStation primaryMetStation = dataSourceManager.getPrimaryMetStation(getCurrentUser(), getCurrentSite());

		EventHistory eventHistory = eventHistoryManager.getEventHistory(getCurrentSite().getId(), scenarioRun.getId(), false);
		scenRunDTO.setLastInGroup(eventHistory.isLastInGroup());
		
		if (!scenarioRun.isCachedScenarioOutputDTO()) {

			if (primaryMetStation != null) {
				scenRunDTO.getScenarioOut().setMetStation(primaryMetStation.asMetStationDTO(mapper));
			} else {
				scenRunDTO.getScenarioOut().setMetStation(manualMetStationDto);
			}
			
			if (scenarioRun.getScenarioType().isFire()) {
				PlumeDTO firePlume = new PlumeDTO();
				firePlume.setLow(getPolygonFromLatLng(scenarioRun.getScenarioOutputDTO().getFireOutDto().getFootprintThermalLow()));
				firePlume.setMedium(getPolygonFromLatLng(scenarioRun.getScenarioOutputDTO().getFireOutDto().getFootprintThermalMed()));
				firePlume.setHigh(getPolygonFromLatLng(scenarioRun.getScenarioOutputDTO().getFireOutDto().getFootprintThermalHigh()));
				scenarioRun.getScenarioOutputDTO().getFireOutDto().setPlume(firePlume);
			} else if (scenarioRun.getScenarioType().isExplosion()) {
				PlumeDTO plume = new PlumeDTO();
				plume.setLow(getPolygonFromLatLng(scenarioRun.getScenarioOutputDTO().getExplOutDto().getFootprintThermalLow()));
				plume.setMedium(getPolygonFromLatLng(scenarioRun.getScenarioOutputDTO().getExplOutDto().getFootprintThermalMed()));
				plume.setHigh(getPolygonFromLatLng(scenarioRun.getScenarioOutputDTO().getExplOutDto().getFootprintThermalHigh()));
				scenarioRun.getScenarioOutputDTO().getExplOutDto().setPlume(plume);
			} else if(!ScenarioType.SAL.equals(scenarioRun.getScenarioType())){
				
				Plume plume = readPlume(scenarioRun);
				List<PlumeDTO> snapshots = new ArrayList<PlumeDTO>();
				List<PlumeDTO> footprints = new ArrayList<PlumeDTO>();
				
				scenarioRun.setPlumeRendered(renderPlume(scenRunDTO.getId(), scenRunDTO.getLocation(), plume, snapshots, footprints));
				scenRunDTO.getScenarioOut().setSnapshots(snapshots);
				scenRunDTO.getScenarioOut().setFootprints(footprints);
				
				double down[] = plume.getDownDistance();
				scenRunDTO.getScenarioOut().setDownwindDistance(new DownwindDistanceDTO(down[0], down[1], down[2]));
				scenRunDTO.getScenarioOut().setVerticalProfile(getVerticalProfileData(scenarioRun));
				scenRunDTO.getScenarioOut().setTimeRings(plume.getTimeRings());
			}
			scenarioRun.setCachedScenarioOutputDTO(true);
			scenarioManager.saveScenarioRun(scenarioRun);
		} else {
			if (getCurrentUser().isRealtime() && scenarioRun.getManualMetData() == null && scenarioRun.getRealTimeMetData() != null) {
				if(ScenarioUtils.licenseDoesNotAlow(scenarioRun.getScenarioType(), siteManager.getSiteSettings(getCurrentSite()))){
					scenRunDTO.getScenarioOut().setNewDataAvailable(new NewDataAvailableDTO(Status.STOP_CHECKING, scenarioRun.getId()));
				}else{
					scenRunDTO.getScenarioOut().setNewDataAvailable(scenarioManager.isNewDataAvailable(scenarioRun.getId(), scenarioRun.getSite().getId(), scenarioRun.getRealTimeMetData(), scenarioRun.getReleaseTime()));
				}
			}
		}

		if (scenarioRun.getRealTimeMetData() != null && !scenarioRun.getRealTimeMetData().isEmpty()) {
			scenRunDTO.setRealMetData(mapper.mapAsList(scenarioRun.getRealTimeMetData().get(0).getMetAverages(), MetDataDTO.class));
			scenRunDTO.setRealMet(mapper.map(scenarioRun.getRealTimeMetData().get(0), MetStationDTO.class));
		}
		
		if (scenRunDTO.getScenarioType() == ScenarioType.SAL ||scenRunDTO.getScenarioType() == ScenarioType.ABC) {
			if (scenarioRun.getSensors() != null && !scenarioRun.getSensors().isEmpty()) {
				Map<Integer, List<SensorAverageDTO>> avgs = new HashMap<Integer, List<SensorAverageDTO>>();
				List<SensorModeling> selectedSensors = scenarioRun.getSensors().stream().filter(p->p.isSelectedInInput())
						.collect(Collectors.toList());
				for (SensorModeling sen :selectedSensors) {
					avgs.put(sen.getId(), new ArrayList<SensorAverageDTO>());
					avgs.get(sen.getId()).addAll(mapper.mapAsList(sen.getAverages(), SensorAverageDTO.class));
				}
				
				//TODO Cristi: review code bellow and above because much of it is not need it (EG: working with DTO objects and dates conversions) 
				Map<Integer,Double> ret = new HashMap<Integer, Double>();
				for(Entry<Integer,List<SensorAverageDTO>> e:avgs.entrySet()){
					List<SensorAverageDTO> sensorAverages = e.getValue();
					for(SensorAverageDTO senAvgDto: sensorAverages)
						senAvgDto.setDateTaken(DateUtils.convertToTimezone(senAvgDto.getDateTaken(), getCurrentSite().getTimeZone()));
					
					Double max = null;
					if(!sensorAverages.isEmpty()){
						SensorAverageDTO avgDto = Collections.max(sensorAverages, new Comparator<SensorAverageDTO>() {
							@Override
							public int compare(SensorAverageDTO o1, SensorAverageDTO o2) {
								if(o1.getValue()==null || o2.getValue()==null){
									return 0;
								}
								return o1.getValue().compareTo(o2.getValue());
							}
						});
						max = avgDto.getValue();
					}
					
					ret.put(e.getKey(), max);
				}
					
				((ScenarioRunSensorInputDTO) scenRunDTO).setSelectedSensorAverage(ret);
			}

		}

		for (SensorDTO sen :scenRunDTO.getSensors()) {
			if(sen.getCurrentAverage()!=null && sen.getCurrentAverage().getDateTaken()!=null) {
				sen.getCurrentAverage().setDateTaken(DateUtils.convertToTimezone(sen.getCurrentAverage().getDateTaken(), getCurrentSite().getTimeZone()));
				sen.setName(SensorUtils.getNameWithTime(SensorUtils.cleanName(sen.getName()),sen.getCurrentAverage().getDateTaken()));
			}
		}
		
		ChemicalDetailsDTO chemDto = getChemical(scenarioRun.getScenario().getEmissionSource().getChemical().getId(), true, true);
		scenRunDTO.setChemical(chemDto.toChemicalDTO());
		scenRunDTO.getScenarioOut().setChemical(chemDto);
		updateChemicalIfNeedIt(chemDto, scenRunDTO.getScenarioOut().getChemicalMatrix());
		scenRunDTO.setReleaseTime(DateUtils.convertToTimezone(utcReleaseTime, getCurrentSite().getTimeZone()));
		return scenRunDTO;
	}
	
	//TODO Implement this functions for reports after ABC Open Patch with Mulctiple chemicals
	private void updateChemicalIfNeedIt(ChemicalDetailsDTO chemDto, List<double[]> chemicalMatrix) {
		if(chemicalMatrix!=null){
			for(double[] line : chemicalMatrix) {
				for(MixtureChemicalDTO mix : chemDto.getMixtureChemicals()) {
					if(mix.getCompChemical().getId().equals(new Integer(new Double(line[0]).intValue()))) {
						mix.setCompAmount(line[1]);
						mix.setCompChemical(chemDto.getMixtureChemicalDetails().get(mix.getCompChemical().getId()));
					}
				}
			}
			List<MixtureChemical> list = mapper.mapAsList(chemDto.getMixtureChemicals(), MixtureChemical.class);
			ChemicalDetailsDTO result = ChemicalUtils.calcMixtureProps(list, chemDto.getMixtureCompositionAmount());
			chemDto.setLiqProp(result.isLiqProp());
			chemDto.setmWeight(result.getmWeight());
			chemDto.setcTemp(result.getcTemp());
			chemDto.setcPressure(result.getcPressure());
			chemDto.setcVolume(result.getcVolume());
			chemDto.setbPoint(result.getbPoint());
			chemDto.setCPVAPA(result.getCPVAPA());
			chemDto.setCPVAPB(result.getCPVAPB());
			chemDto.setCPVAPC(result.getCPVAPC());
			chemDto.setCPVAPD(result.getCPVAPD());
			chemDto.setHeatForm(result.getHeatForm());
			chemDto.setHeatComb(result.getHeatComb());
			chemDto.setHeatVapor(result.getHeatVapor());
			chemDto.setReactivity(result.getReactivity());
			chemDto.setUperExplm(result.getUperExplm());
			chemDto.setLowerExplm(result.getLowerExplm());
			chemDto.setsTension(result.getsTension());
			chemDto.setViscosity(result.getViscosity());
			chemDto.setEnthNbp(result.getEnthNbp());
			chemDto.setEnthNbp10(result.getEnthNbp10());
			chemDto.setEnthNbp20(result.getEnthNbp20());
			chemDto.setDensNbp(result.getDensNbp());
			chemDto.setDensNbp5(result.getDensNbp5());
			chemDto.setTemp400(result.getTemp400());
			chemDto.setnCarbon(result.getnCarbon());
			chemDto.setnHydrogen(result.getnHydrogen());
			chemDto.setnOxygen(result.getnOxygen());
			chemDto.setnNitrogen(result.getnNitrogen());
			chemDto.setnHalogens(result.getnHalogens());
			chemDto.setnSulfur(result.getnSulfur());
			
			for(double[] line : chemicalMatrix) {
				for(MixtureChemicalDTO mix : chemDto.getMixtureChemicals()) {
					if(mix.getCompChemical().getId().equals(new Integer(new Double(line[0]).intValue()))) {
						mix.setCompAmount(line[1]*100);
					}
				}
			}
		}
	}

	private List<PolygonDTO> getPolygonFromLatLng(List<double[]> footprint) {
		ArrayList<PolygonDTO> list = new ArrayList<PolygonDTO>();
		if (footprint != null && footprint.size() > 0) {
			PolygonDTO poly = new PolygonDTO();
			List<PointDTO> path = new ArrayList<PointDTO>();
			for (double[] lngLat:footprint) {
				path.add(new PointDTO(lngLat[1], lngLat[0]));
			}
			poly.setPath(path);
			list.add(poly);
		}
		return list;
	}

	@Override
	public ScenarioRunDTO getScenarioOutput(int scenarioRunId) {
	    ScenarioRun scenarioRun = scenarioManager.getScenarioRun(scenarioRunId);
	    return getScenarioOutput(scenarioRun);
	}

	private boolean renderPlume(Integer scenRunId, PointDTO releaseLocation, Plume plume, List<PlumeDTO> snapshots, List<PlumeDTO> footprints) {
		boolean terminated = false;
		try {
			long startTime = System.currentTimeMillis();
	        ExecutorService executor = Executors.newFixedThreadPool(6);
	        Hashtable<Integer, PlumeDTO> snapshotsMap = new Hashtable<Integer, PlumeDTO>();
	        Hashtable<Integer, PlumeDTO> footprintsMap = new Hashtable<Integer, PlumeDTO>();
			for(int i=0;i<24;i++) {
				final int k = i;
				executor.execute(new Runnable() {
					
					@Override
					public void run() {
						snapshotsMap.put(k,getPlume(releaseLocation, plume, k, true));
					}
				});
				executor.execute(new Runnable() {
					 
					@Override
					public void run() {
						footprintsMap.put(k,getPlume(releaseLocation, plume, k, false));
					}
				});
			}
			executor.shutdown();
			
			terminated = executor.awaitTermination(3, TimeUnit.MINUTES);
			if(!terminated) {
				executor.shutdownNow();
				System.out.println("Executed shutdownNow for scenarioId: " + scenRunId );
				throw new RuntimeException("Plume not drawn is 3 minutes. Please try again or contact SAFER Support");
			}
			long estimatedTime = System.currentTimeMillis() - startTime;
			System.out.println("Plume render took " + estimatedTime/1000);
			
			for(int i=0;i<24;i++) { 
				snapshots.add(snapshotsMap.get(i));
				footprints.add(footprintsMap.get(i));
			}
			
		}catch(Exception e) {
			e.printStackTrace();
		}
		return terminated;
	}

	@Override
	public void saveScenarioName(int scenarioRunId, String scenarioName) throws Exception {
		scenarioName = HtmlUtils.escapeHtml(scenarioName);
		scenarioManager.updateScenarioRunName(scenarioRunId, scenarioName);
	}

	@Override
	public List<EventHistoryDTO> getHistoryFor(EventHistoryDTO hist) throws RuntimeException {
		if(hist.isErg()||(!hist.isErg() && (hist.getGoupId()==hist.getEventId()))){
			throw new RuntimeException("No History Found");
		}
		return eventHistoryManager.getHistoryFor(hist, getCurrentSite());
	}
	
	@Override
	public EventHistoryDTO getEventHistory(Integer eventHistoryId) {
		return mapper.map(eventHistoryManager.getEventHistoryById(eventHistoryId), EventHistoryDTO.class);
	}



	/**
	 * get's the latest 1 minute average dateTaken of all met stations of the current site. If for a met station there is no one minute average, it will look for the latest 5 min average
	 * @return latest dateTaken of the averages of the met stations from current site or null if the site has no met average
	 */
	@Override
	public Date getLatestMetDateTaken() {
		return DateUtils.convertToTimezone(dataSourceManager.getLatestMetDateTaken(getCurrentSite().getId()), getCurrentSite().getTimeZone());
	}

	@Override
	public void setCustomUnitDescriptionForParamPerUser(UnitParam unitParam, UnitDesc unitDesc) {
		unitsManager.setCustomUnitDescriptionForParamPerUser(unitParam, unitDesc,getCurrentSite().getId() ,getCurrentUser().getId());
	}
	
	@Override
	public Units resetUserUnits() {
		unitsManager.clearUserCustomUnits(getCurrentUser().getId(),getCurrentSite().getId());
		Units units = unitsManager.getUnits(getCurrentSite().getId(),getCurrentUser().getId());
		return units;
	}

	@Override
	public ListLoadResult<String> getUnitNames(ListLoadConfig loadConfig) {
		return new ListLoadResultBean<String>(emissionManager.getUnitNames(getCurrentSite().getId()));
	}
	
	@Override
	public List<SensorAverageDTO> getSensorAverages(Date refDate, ChemicalDTO chemical, Integer id, ScenarioType scenarioType, boolean isOutput, boolean isRaeMonitoring) {
		Date startDate = new Date();
		Date endDate = new Date(); 
		
		refDate = DateUtils.convertFromTimezoneToUtc(refDate, getCurrentSite().getTimeZone());
		
		List<SensorAverageDTO> avgs = new ArrayList<>();
		Sensor sen = dataSourceManager.getSensor(id);
		
		Chemical chem = null;
		if(scenarioType != null && scenarioType.isRelease()) {
			startDate = refDate;
			endDate = new Date(refDate.getTime()+2*60*60*1000);
			chem = chemicalsManager.findById(chemical.getId());
		}else if(scenarioType == ScenarioType.SAL) {
			startDate = new Date(refDate.getTime()-1*60*60*1000);
			endDate = new Date(refDate.getTime()+1*60*60*1000);
			chem = chemicalsManager.findById(chemical.getId());
		}else {
			startDate = new Date(refDate.getTime()-2*60*60*1000);
			endDate = refDate;
			if (chemical != null) {
				chem = chemicalsManager.findById(chemical.getId());
			}
		}
		avgs = mapper.mapAsList(dataSourceManager.getSensorAveragesAsc(sen, startDate, endDate), SensorAverageDTO.class);
		for(SensorAverageDTO avg : avgs) {
			if(scenarioType != null || isRaeMonitoring) {
				Double senVal = SensorUtils.getSensorValue(sen, avg.getValue(), chem, false);
				if (senVal != null) { //the chemical is not applicable to the selected sensor
					avg.setValue(senVal);
				}
			}
			avg.setDateTaken(DateUtils.convertToTimezone(avg.getDateTaken(), getCurrentSite().getTimeZone()));
		}
		return avgs;
	}
	
	private SensorAveragesWrapper getSensorAverages(Date dateTaken,Chemical ch, double min , boolean isAbc, Site site) {
		SensorAveragesWrapper wrap = new SensorAveragesWrapper();
		if(isAbc){
			//TODO Cristi - filter results extract from the database only the sensors for selected chemical
			wrap = dataSourceManager.calculateABCSensors(site, DateUtils.convertFromTimezoneToUtc(dateTaken, site.getTimeZone()),ch,min);
		}else{
			wrap.setSensors(dataSourceManager.getSensors(site));
			wrap.setSensorAverages(dataSourceManager.getSensorAverages(site, DateUtils.convertFromTimezoneToUtc(dateTaken, site.getTimeZone()),ch,min,isAbc));
		}
		return wrap;
	}
	
	private SensorAveragesWrapperDTO getEmptySensorAverageWrapperDTO(){
		SensorAveragesWrapperDTO wrapper = new SensorAveragesWrapperDTO();
		Map<Integer, SensorAverageDTO> retSenAvgDtoMap = new HashMap<Integer, SensorAverageDTO>();
		wrapper.setSensorAverages(retSenAvgDtoMap);
		Map<Integer,SensorDTO> retSenDtoMap = new HashMap<>();
		wrapper.setSensors(retSenDtoMap);
		return wrapper;
	}
	private SensorAveragesWrapperDTO getSensorInputAverages(Date releaseTime, ChemicalDTO chemical , Double min, boolean isAbc, Site site) {
		if (releaseTime == null || chemical == null) {
			return getEmptySensorAverageWrapperDTO();
		}
		Chemical chemicalDetails = chemicalsManager.findById(chemical.getId());
		
		if(min==null || min<0){
			Double d = getIsoplethsForChemicalAndSite(site, chemicalDetails);
			min = d==null?null:d/100;
		}
		
		if(min==null|| min<0){
			Double d = getIsoplethsForChemicalAndSite(null, chemicalDetails);
			min = d==null?null:d/100;
		}
		
		//TODO chemical.isSensorMixture() is not set on update and run . Why ???
		if(chemicalDetails.isSensorMixture()){ //OPMG works only with OP sensors
			min = -1.0;
		}
		 
		SensorAveragesWrapper wrap = getSensorAverages(releaseTime,chemicalDetails,min==null?0.00000001:min,isAbc , site);
		return getSensorComputedValues(isAbc, false, site, chemicalDetails, wrap);
	}

	private SensorAveragesWrapperDTO getSensorComputedValues(boolean isAbc, boolean isMonitoring, Site site, Chemical chemicalDetails,
			SensorAveragesWrapper wrap) {
		List<Sensor> sensors = wrap.getSensors();
		List<SensorAverage> sensorAverages = wrap.getSensorAverages();

		SensorAveragesWrapperDTO wrapper = getEmptySensorAverageWrapperDTO();
		Map<Integer, SensorAverageDTO> retSenAvgDtoMap = wrapper.getSensorAverages();
		Map<Integer,SensorDTO> retSenDtoMap = wrapper.getSensors();
		
		Map<Integer, SensorAverage> mapSensorIdAverage = new HashMap<Integer, SensorAverage>();
		for (SensorAverage sa:sensorAverages){
			mapSensorIdAverage.put(sa.getSensor().getId(), sa);
		}
		
		List<SensorDTO> sensorsDto = mapper.mapAsList(sensors, SensorDTO.class);
		for(SensorDTO sensor : sensorsDto) {
			//TODO Cristi Improve by testing LEL and VOC chemicals
			boolean sameChemical = chemicalDetails.getId().equals(sensor.getChemicalId()) || chemicalDetails.getId().equals(sensor.getMixtureChemicalId());
			if(mapSensorIdAverage.get(sensor.getId())==null || 
					((SensorType.GENERIC.equals(sensor.getType()) || SensorType.EC.equals(sensor.getType()) || SensorType.LIGHT.equals(sensor.getType())) &&  !sameChemical) ){
				continue;
			}
			
			retSenDtoMap.put(sensor.getId(),sensor);
		}
 		
 		Map<Integer, List<SensorForValueComputationI>> raeSensors = new HashMap<Integer, List<SensorForValueComputationI>>();
 		Map<Integer, Map<Integer, Double>> raeSensorsValues = new HashMap<Integer, Map<Integer, Double>>();
 		
		for (Sensor s:sensors) {
			SensorAverage avg = mapSensorIdAverage.get(s.getId());
			if (avg!= null && avg.getValue() != null && (avg.getValue() > 0||chemicalDetails.isSensorMixture())) {
				if (s.getSensorRae() == null) {
					//non rae modeling sensors are only the EC sensors
					boolean sameChemical = chemicalDetails.getId().equals(s.getChemicalId()) || chemicalDetails.getId().equals(s.getMixtureChemicalId());
					if (isMonitoring || 
							((SensorType.GENERIC == s.getType() || SensorType.EC.equals(s.getType()) || SensorType.LIGHT.equals(s.getType())) && sameChemical)) { 
						avg.setDateTaken(avg.getDateTaken());
						avg.setValue(avg.getValue());
						retSenAvgDtoMap.put(s.getId(), mapper.map(avg, SensorAverageDTO.class));
					}
				} else {
					int raeId = isAbc?s.getSensorRae().getId()*1000 + avg.getSensor().getId()%1000:s.getSensorRae().getId();
					if (!raeSensors.containsKey(raeId)) {
						raeSensors.put(raeId, new ArrayList<SensorForValueComputationI>());
					}
					raeSensors.get(raeId).add(s);
					
					if (!raeSensorsValues.containsKey(raeId)) {
						raeSensorsValues.put(raeId, new HashMap<Integer, Double>());
					}
					raeSensorsValues.get(raeId).put(s.getId(), avg.getValue());
				}
			}
		}
		
		for (Integer raeId:raeSensors.keySet()) {
			SensorRaeValue val = SensorUtils.getSensorRaeValue(raeSensors.get(raeId), raeSensorsValues.get(raeId), chemicalDetails);
			if (val != null || isMonitoring) {
				SensorAverageDTO avg;
				if (val != null) {
					avg = new SensorAverageDTO();
					avg.setDateTaken(mapSensorIdAverage.get (val.getSensorId()).getDateTaken());
					avg.setLocation(mapper.map(mapSensorIdAverage.get(val.getSensorId()).getLocation(), PointDTO.class));
					avg.setAverageValue(mapSensorIdAverage.get(val.getSensorId()).getValue());
					avg.setValue(val.getValue());
					avg.setRaeValue(true);
					avg.setChemicalId(chemicalDetails.getId());
					retSenAvgDtoMap.put(val.getSensorId(), avg);
				}
				//put the values for the rest of the sensors from rae unit so I can display them in tooltip
				for (SensorForValueComputationI s:raeSensors.get(raeId)){
					if (val == null || !s.getId().equals(val.getSensorId())) {//the sensor is not the one that gives the value to rae
						Double senVal = SensorUtils.getSensorValue(s, raeSensorsValues.get(raeId).get(s.getId()), chemicalDetails, false);
						if (senVal != null) {
							avg = new SensorAverageDTO();
							avg.setDateTaken(mapSensorIdAverage.get (val.getSensorId()).getDateTaken());
							avg.setLocation(mapper.map(mapSensorIdAverage.get(s.getId()).getLocation(), PointDTO.class));
							avg.setAverageValue(mapSensorIdAverage.get(s.getId()).getValue());
							avg.setValue(senVal);
							avg.setChemicalId(chemicalDetails.getId());
							retSenAvgDtoMap.put(s.getId(), avg);
						} else {
							avg = new SensorAverageDTO();
							avg.setDateTaken(mapSensorIdAverage.get(s.getId()).getDateTaken());
							avg.setLocation(mapper.map(mapSensorIdAverage.get(s.getId()).getLocation(), PointDTO.class));
							avg.setAverageValue(mapSensorIdAverage.get(s.getId()).getValue());
							retSenAvgDtoMap.put(s.getId(), avg);
						}
					}
				}
			}
		}
		
		for(Entry<Integer, SensorAverageDTO> e: retSenAvgDtoMap.entrySet()){
			e.getValue().setDateTaken( DateUtils.convertToTimezone(e.getValue().getDateTaken(), site.getTimeZone()));
		}
		
		for(Entry<Integer, SensorDTO>  e: wrapper.getSensors().entrySet()){
			try{ 
				//if(e.getValue().getSensorRae()==null){
					SensorAverageDTO avg = wrapper.getSensorAverages().get(e.getKey());
					if(avg!=null){
						e.getValue().setLocation(avg.getLocation());
						e.getValue().setCurrentAverage(avg);
					}
				//}
			}catch(Exception e11){
				e11.printStackTrace();
			}
		}
		
		return wrapper;
	}
	
	/**
	 * for rae it will put in map only the sensor used to compute the rae value
	 */
	@Override
	public  SensorAveragesWrapperDTO getSensorInputAverages(Date releaseTime, ChemicalDTO chemical , Double min, boolean isAbc) {
		return getSensorInputAverages(releaseTime, chemical, min, isAbc, getCurrentSite());
	}

	private Double getIsoplethsForChemicalAndSite(Site site, Chemical chemical) {
		List<ConcentrationIsopleths> concIsoList = concIsoplethRepos.findBySiteAndChemicalAndHiddenFalse(site, chemical);
		if(concIsoList!=null && !concIsoList.isEmpty()) {
			for(ConcentrationIsopleths existingIsopleth : concIsoList) {
				if(existingIsopleth!=null && ConcentrationIsoplethsType.TOXICITY.toString().equalsIgnoreCase(existingIsopleth.getType())) {
					return existingIsopleth.getLow();
				}
			}
		}
		return null;
	}

	@Override
	public AutoSelectResponseDTO getAutoChemical(Date dateTaken, List<PointDTO> range, boolean isAbc, PointDTO clickLocation, ScenarioType scenarioType, MetDataDTO manualMet) {
		

		AutoSelectResponseDTO response = new AutoSelectResponseDTO();
		SensorAveragesWrapper wrap = getSensorAverages(dateTaken,null,0d,isAbc, getCurrentSite());
		List<Sensor> sensors = wrap.getSensors();
		List<SensorAverage> sensorAverages =wrap.getSensorAverages();
		boolean allSensorsDoNotHaveChemicals = true;
		Map<Sensor, SensorAverage> mapSA = new HashMap<Sensor, SensorAverage>();

		Geometry poly = GeometryUtils.createPolygon(range, true);
		for (SensorAverage sa:sensorAverages){
			if(isAbc && range!=null) {
				if(poly==null || sa.getLocation()==null) {
					continue;
				}
				boolean isLight = sa.getSensor().getType()!=null && sa.getSensor().getType().isLight();
				if(isLight) {
					if(sa.getReflectorLocation()==null) {
						continue;
					}
					PointConverter pc = new PointConverter();
					PointDTO start = pc.convertFrom(sa.getLocation(),null);
					PointDTO end = pc.convertFrom(sa.getReflectorLocation(),null);
					Geometry line = GeometryUtils.createLine(start,end);
					if(!poly.intersects(line)) {
						continue;
					}
				}else if(!poly.contains(sa.getLocation())) {
					continue;
				}
			}
			mapSA.put(sa.getSensor(), sa);
		}
		
		boolean vocInAlarm = false, lelInAlarm = false;
		Map<Chemical, Integer> chemSensorCount = new HashMap<>(); 
		Set<Chemical> mixtureChemicals = new HashSet<>();
		for (Sensor s:sensors) {
			SensorAverage avg = mapSA.get(s);
			if (avg == null || avg.getValue() == null) {
				continue;
			}
			boolean isNotDeadband = s.getAlarmValues() == null || s.getAlarmValues().getDeadband() == null || Math.abs(avg.getValue()) >= s.getAlarmValues().getDeadband();
			if(s.getChemical()!=null && isNotDeadband) {
				allSensorsDoNotHaveChemicals=false;
			}
			if((SensorType.GENERIC == s.getType() || SensorType.EC == s.getType() || SensorType.LIGHT == s.getType()) && isNotDeadband) {
				Chemical chem = s.getChemical();
				if(SensorType.LIGHT == s.getType() && s.getMixtureChemical()!=null) {
					chem = s.getMixtureChemical();
					mixtureChemicals.add(chem);
				}
				if(!chemSensorCount.containsKey(chem)) {
					chemSensorCount.put(chem, 0);
				}
				chemSensorCount.put(chem, chemSensorCount.get(chem) + 1);
			}else if(SensorType.VOC == s.getType()) {
				vocInAlarm = true;
			}else if(SensorType.LEL == s.getType()) {
				lelInAlarm = true;
			}
		}
		if(chemSensorCount.size() == 1) {
			Entry<Chemical,Integer> entry = chemSensorCount.entrySet().iterator().next();
			boolean validVoc = !vocInAlarm || (vocInAlarm && entry.getKey().detectedByVoc());
			boolean validLel = !lelInAlarm || (lelInAlarm && entry.getKey().detectedByLel());
			if(entry.getValue() >= 1 && validVoc && validLel) {
				response.setChemical(mapper.map(entry.getKey(), ChemicalDTO.class));
			}
		}else if(chemSensorCount.size() > 1 && mixtureChemicals.size()==1){
			Chemical mixture = mixtureChemicals.iterator().next();
			String[] split = mixture.getMixtureChemicalIds().split(",");
			Set<Integer> mixtureChemicalIds = new HashSet<>();
			for(String id : split) {
				mixtureChemicalIds.add(Integer.valueOf(id));
			}
			boolean onlyMixtureCompatible = true;
			for(Entry<Chemical,Integer> entry : chemSensorCount.entrySet()) {
				if(!entry.getKey().equals(mixture) && !mixtureChemicalIds.contains(entry.getKey().getId())) {
					onlyMixtureCompatible = false;
				}
			}
			if(onlyMixtureCompatible) {
				response.setChemical(mapper.map(mixture, ChemicalDTO.class));
			}
		}else if(chemSensorCount.isEmpty()) {
			response.setNoSensors(true);
		}
		if(response.getChemical()==null && allSensorsDoNotHaveChemicals) {
			List<MetAverageDTO> metAverages = getSensorInputMet(dateTaken, clickLocation, null, scenarioType);
			MetDataDTO metData = manualMet;  
			if(metData == null) {
				MetAverageDTO metAverage = metAverages != null && metAverages.size() > 0 ? metAverages.get(metAverages.size()-1) : null;
				metData = metAverage.getMetData();
			}
			if(metData!=null && metData.getWindDirection()!=null && metData.getWindSpeed()!=null && metData.getWindSpeed() > Corridor.MIN_WIND_SPEED){
				List<EmissionSource> ems = emissionManager.getEmissionRepository().findBySiteAndHiddenFalseAndLocationNotNull(getCurrentSite());
				Chemical chemical = SalUtils.autoSelectChemicalFromCorridors(ems, mapSA.values(), metData, getSiteSettings(getCurrentSite().getId()).getCorridorLengthTimeInterval());
				if(chemical!=null) {
					boolean validVoc = !vocInAlarm || (vocInAlarm && chemical.detectedByVoc());
					boolean validLel = !lelInAlarm || (lelInAlarm && chemical.detectedByLel());
					if(validVoc && validLel) {
						response.setChemical(mapper.map(chemical,ChemicalDTO.class));
					}
				}
			}
		}
		return response;
	}

	
	@Override
	public List<MetAverageDTO> getSensorInputMet(Date dateTaken, PointDTO clickLocation, List<SensorDTO> selectedSensors, ScenarioType scenarioType) {
		Date utcDate = DateUtils.convertFromTimezoneToUtc(dateTaken, getCurrentSite().getTimeZone());
		List<MetStationModelling> metStations = dataSourceManager.createMetStationModellingInstances(utcDate, new GeometryFactory().createPoint(new Coordinate(clickLocation.getLatitude(), clickLocation.getLongitude())), 
				null, null, getCurrentUser());
		List<MetAverageDTO> list = new ArrayList<MetAverageDTO>();
		if (metStations.size() > 0) {
			MetStationModelling met = metStations.get(0);
			if (met.getMetAverages() != null && met.getMetAverages().size() > 0) {
				MetStationDTO metStationDTO = unitsManager.toCustom(mapper.map(met, MetStationDTO.class), getCurrentSite().getId(), getCurrentUser().getId());
				int n = scenarioType == ScenarioType.SAL ? 1 : met.getMetAverages().size();
				for (int i = 0; i<n; i++) {
					MetAverageDTO dto = new MetAverageDTO();
					dto.setMetStation(metStationDTO);
					dto.setDateTaken(new Date(dateTaken.getTime() + i*5*60*1000));
					dto.setMetData(unitsManager.toCustom(mapper.map(met.getMetAverages().get(i), MetDataDTO.class), getCurrentSite().getId(), getCurrentUser().getId()));
					list.add(dto);
				}
			}
		}
		return list;
	}

	@Override
	public PagingLoadResult<SensorHistoryDTO> getSensorHistoryData(Date start, Date end, List<SensorDTO> sensors,
			FilterPagingLoadConfig config) {
		//PageRequest pageable;
		
		Date startUtc = DateUtils.trimToMinutes(DateUtils.convertFromTimezoneToUtc(start, getCurrentSite().getTimeZone()));
		Date endUtc = DateUtils.trimToMinutes(DateUtils.convertFromTimezoneToUtc(end, getCurrentSite().getTimeZone()));
		
		List<Integer> sensorIds = new ArrayList<Integer>();
		for (SensorDTO s:sensors) {
			sensorIds.add(s.getId());
		}
		
		//the averages are ordered desc
		Date endPage = dataSourceManager.getSensorHistoryDateAtOffset(startUtc, endUtc, sensorIds, config.getOffset());
		if (endPage == null) {
			endPage = endUtc;
		}
		Date startPage = dataSourceManager.getSensorHistoryDateAtOffset(startUtc, endUtc, sensorIds, config.getOffset()+config.getLimit()-1);
		if (startPage == null) {
			startPage = startUtc;
		}

		List<SensorAverage> averages = dataSourceManager.getSensorsAverages(startPage, endPage, sensorIds);
		List<SensorHistoryDTO> sensorsHistory = new ArrayList<SensorHistoryDTO>();
		Map<Date, SensorHistoryDTO> shMap = new HashMap<Date, SensorHistoryDTO>();
		for (SensorAverage sa:averages) {
			SensorHistoryDTO dto;
			if (!shMap.containsKey(sa.getDateTaken())) {
				dto = new SensorHistoryDTO();
				dto.setDateTaken(DateUtils.convertToTimezone(sa.getDateTaken(), getCurrentSite().getTimeZone()));
				dto.setSensorAverages(new HashMap<Integer, SensorAverageDTO>());
				shMap.put(sa.getDateTaken(), dto);
				sensorsHistory.add(dto);
			} else {
				dto = shMap.get(sa.getDateTaken());
			}
			dto.getSensorAverages().put(sa.getSensor().getId(), mapper.map(sa, SensorAverageDTO.class));
			
		}
		
		return new PagingLoadResultBean<SensorHistoryDTO>(sensorsHistory, dataSourceManager.getSensorHistoryTotal(startUtc, endUtc, sensorIds).intValue(), config.getOffset());
	}

	@Override
	public String createSensorHistoryCSVFile(Date start, Date end, List<SensorDTO> sensors) {
		if(start==null || end==null || sensors == null || sensors.isEmpty() ){
			throw new RuntimeException("No Sensor Average Available !");
		}
		Date startUtc = DateUtils.trimToMinutes(DateUtils.convertFromTimezoneToUtc(start, getCurrentSite().getTimeZone()));
		Date endUtc = DateUtils.trimToMinutes(DateUtils.convertFromTimezoneToUtc(end, getCurrentSite().getTimeZone()));
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH mm");
		final String sensorHistoryOutPath = getBaseFileFolderForUserFiles("report")+"Sensor History "+format.format(start)+ " - " + format.format(end) + ".csv";
		
		try {
			return properties.getApplicationUrl() + Config.DOWNLOAD_FILE_SERVICE_PATH+"?action=downloadFile&downloadType=report&fileName=" 
				+ createSensorHistoryCSV(startUtc, endUtc, sensors, sensorHistoryOutPath, null,false);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());
		}
	}

	private String createSensorHistoryCSV(final Date startDate, final Date endDate, List<SensorDTO> sensors, 
			String sensorHistoryOutPath ,ZipOutputStream zos, boolean isReport) throws IOException{
		
		
		CSVWriter writer = new CSVWriter(new FileWriter(sensorHistoryOutPath), ',');
		int columnCount = sensors.size()*2+1;
		String[] entries = new String[columnCount];
		
		RaeMonitoringChemical raeChemical = siteManager.getRaeMonitoringChemical(getCurrentSite(), getCurrentUser());
		
		entries[0] = "Date Taken";
		
		Map<Integer, Integer> sensorToCsvEntry = new HashMap<Integer, Integer>();
		for (int i = 0;i<sensors.size(); i++) {
			SensorDTO sensor = sensors.get(i);
			
			String sensorName = sensor.getFullName();
			String sensorUnit =  sensor.getUnitLabel() != null && !sensor.getUnitLabel().isEmpty() ? sensor.getUnitLabel() : "ppm";
			
			if (isReport && raeChemical != null && raeChemical.getChemical() != null && (SensorType.LEL.equals(sensor.getType()) || SensorType.VOC.equals(sensor.getType()))){
				sensorUnit = "ppm";
				sensorName += " - " + raeChemical.getChemical().getFormula();
			}
			entries[i*2+1] = sensorName + " (" + sensorUnit + ")";
			entries[i*2+2] = sensorName + " - Location";
			
			sensorToCsvEntry.put(sensor.getId(), i*2+1);
		}
		writer.writeNext(entries);
		
		List<SensorAverage> averages = dataSourceManager.getSensorsAverages(startDate, endDate, new ArrayList<Integer>(sensorToCsvEntry.keySet()));
		SimpleDateFormat f1 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		
		if (averages.size() > 0) {
			String[] row = new String[columnCount];
			Date currentDateTaken = averages.get(0).getDateTaken();
			row[0] = f1.format(DateUtils.convertToTimezone(currentDateTaken, getCurrentSite().getTimeZone()));
			for (SensorAverage sa : averages){
				if (!currentDateTaken.equals(sa.getDateTaken())){
					writer.writeNext(row);
					currentDateTaken = sa.getDateTaken();
					row = new String[columnCount];
					row[0] = f1.format(DateUtils.convertToTimezone(currentDateTaken, getCurrentSite().getTimeZone()));
				}
				int sensorId = sa.getSensor().getId();
				Sensor sen = sa.getSensor();
				Double value = sa.getValue();
				if (isReport && raeChemical != null && raeChemical.getChemical() != null && (SensorType.LEL.equals(sen.getType()) || SensorType.VOC.equals(sen.getType()))){
					value = SensorUtils.getSensorValue(sen,value , raeChemical.getChemical(),false);
				}
				row[sensorToCsvEntry.get(sensorId)] = String.valueOf(FormatUtils.getFormatted(value, 2));
				row[sensorToCsvEntry.get(sensorId)+1] = sa.getLocation()!=null ? String.valueOf(PointDTO.toString(sa.getLocation().getX(), sa.getLocation().getY())) : "";
			}
			writer.writeNext(row);
		}
		
		writer.close();
		
		if(zos!=null)
			ZipUtils.addToZipFile(sensorHistoryOutPath, zos);
		
		return FilenameUtils.getName(sensorHistoryOutPath);
	}

	@Override
	public Boolean resetPassword(String email) {
		if(loginService.forgotPasswordPC(email).equals("ok")){
		      return true;
		}
		return false;
	}
	@Override
	public String resetPasswordPC(String email) {

		  return loginService.forgotPasswordPC(email);

		
	}
	@Override
	public ChemicalDetailsDTO saveRaeMonitoringChemical(Integer chemicalId) {
		Chemical chemical;
		if (chemicalId != null) {
			chemical = chemicalsManager.findById(chemicalId);
		} else {
			chemical = null;
		}

		RaeMonitoringChemical raeChemical = siteManager.getRaeMonitoringChemical(getCurrentSite(), getCurrentUser());
		if (raeChemical != null) {
			raeChemical.setChemical(chemical);
			raeChemical.setModifiedDate(new Date());
		} else if (chemical != null){
			raeChemical = new RaeMonitoringChemical();
			raeChemical.setChemical(null);
			raeChemical.setUser(getCurrentUser());
			raeChemical.setSite(getCurrentSite());
		}
		
		if (raeChemical != null) {
			siteManager.saveRaeMonitoringChemical(raeChemical);
			if (chemical != null) {
				return chemicalsManager.getChemicalDetails(chemicalId, getCurrentSite());
			}
		}
		return null;
	}

	@Override
	public boolean updateRaeSensorInterfaceStatus() {
		List<SensorInterface>  raeSensorInterfaces = dataSourceManager.getSensorInterfaceEnabledBySiteAndTypeRae(getCurrentSite());
		return raeSensorInterfaces!=null && raeSensorInterfaces.size()>0;
	}

	@Override
	public TipOfTheDayDTO selectOneTipOfTheDay(Integer id, TipOfTheDayType tipOfTheDayType) {
		TipOfTheDay tip = tipOfTheDayManager.selectOneTipOfTheDay(id);
		return mapper.map(tip, TipOfTheDayDTO.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean saveGridChangedTipOfTheDay(List<TipOfTheDayDTO> tips) throws RuntimeException {
		if(!getCurrentUser().isSaferAdmin()){
			throw new RuntimeException("Not Allowed");
		}
		tips= (List<TipOfTheDayDTO>)HtmlUtils.escapeHtml(tips);
	    return tipOfTheDayManager.saveGridChangedFeedback(tips);
	}
	
	
	/**
	 * each direction will be a series, having its value and the next direction value the computed percentage and all the other directions 0.
	 */
	@Override
	public ListLoadResult<WindRoseDTO> getWindRoseData(Date start, Date end, MetAverageType averageType, int sectorsCount, Integer metStationId) throws FieldValidationException {
		start = DateUtils.convertFromTimezoneToUtc(start, getCurrentSite().getTimeZone());
		end = DateUtils.convertFromTimezoneToUtc(end, getCurrentSite().getTimeZone());
		if (start.after(end)) {
			throw new FieldValidationException("Start has to be before end");
		}
		long interval = end.getTime()-start.getTime();
		switch (averageType) {
		case FIVE_MINUTE_AVERAGE:
			if (interval > Config.WINDROSE_MAX_INTERVAL_5MIN*24*3600*1000) {
				throw new FieldValidationException("Maximum interval for 5-minute averages is "  + Config.WINDROSE_MAX_INTERVAL_5MIN + " days");
			}
			break;
		case ONE_HOUR_AVERAGE:
			if (interval > Config.WINDROSE_MAX_INTERVAL_1HOUR*24*3600*1000) {
				throw new FieldValidationException("Maximum interval for 1-hour averages is "  + Config.WINDROSE_MAX_INTERVAL_1HOUR + " days");
			}
			break;		
		default:
			throw new FieldValidationException("Average type needs to be one hour or five minutes");
		}
		List<WindRoseDTO> list = new ArrayList<WindRoseDTO>();
		
		WindDirection[] incDir;
		
		switch (sectorsCount) {
		case 4:
			incDir = new WindDirection[] {WindDirection.NORTH, WindDirection.EAST, WindDirection.SOUTH, WindDirection.WEST };
			break;
		case 8:
			incDir = new WindDirection[] {WindDirection.NORTH, WindDirection.NORTH_EAST, WindDirection.EAST, WindDirection.SOUTH_EAST, 
					WindDirection.SOUTH, WindDirection.SOUTH_WEST, WindDirection.WEST, WindDirection.NORTH_WEST };
			break;
		case 16:
			incDir = new WindDirection[] {WindDirection.NORTH, WindDirection.NORTH_NORTH_EAST, WindDirection.NORTH_EAST, WindDirection.EAST_NORTH_EAST, WindDirection.EAST,
					WindDirection.EAST_SOUTH_EAST, WindDirection.SOUTH_EAST, WindDirection.SOUTH_SOUTH_EAST, WindDirection.SOUTH, WindDirection.SOUTH_SOUTH_WEST, 
					WindDirection.SOUTH_WEST, WindDirection.WEST_SOUTH_WEST, WindDirection.WEST, WindDirection.WEST_NORTH_WEST, WindDirection.NORTH_WEST, WindDirection.NORTH_NORTH_WEST
					};

			break;
		default:
			throw new FieldValidationException("Count needs to be 4, 8 or 16");
		}
		
		List<Double> windDirection = dataSourceManager.getWindroseWindDirection(start, end, averageType, metStationId, getCurrentSite().getId());
		double sectorAngle = 360.0/sectorsCount;
		double halfSector = sectorAngle/2;
		WindRoseDTO[] dirToDto = new WindRoseDTO[sectorsCount];
		for (int i = 0; i<sectorsCount; i++) {
			dirToDto[i] = new WindRoseDTO(null, sectorAngle, 0);
		}
		
		for (Double dir:windDirection) {
			WindRoseDTO dto = dirToDto[(int) ((dir+halfSector)%360/sectorAngle)];
			dto.setLength(dto.getLength()+1);
		}

		//the chart used to draw the windrose uses 0X axis as the staring axis (E for winddirection). This means that first element from the list is E
		//the pies is draw counter clock wise and the chart clockwise. Length will start with E and go counter clock wise. Axe will start with E and go clock wise.
		//We need to perform some calculations to rotate the axis values and the pie values in a way that will match the windrose
		int startIndex = sectorsCount/4;		
		for (int seriesIndex = 0; seriesIndex<sectorsCount; seriesIndex++) {
			//the drawing starts with east
			int counterclockIndex = seriesIndex == 0 ? 0 : sectorsCount - seriesIndex;
			int dtoIndex = (counterclockIndex + startIndex)%sectorsCount;

			WindRoseDTO dto = dirToDto[dtoIndex];
			dto.setDirectionAxe(incDir[(seriesIndex+startIndex)%sectorsCount]);
			list.add(dto);
		}

		return new ListLoadResultBean<WindRoseDTO>(list);
	}

	@Override
	public DaqUpdatesDTO getLastestModifiedDaqUpdate() {
		return mapper.map(daqUpdatesRepository.findTop1ByEnvOrderByModifiedDateDesc(version.getEnv()), DaqUpdatesDTO.class);
	}

	@Override
	public ListLoadResult<LightningHistoryChartDTO> getLightningHistoryData(Date start, Date end, Double alarm1, Double alarm2, Integer sensorId) throws FieldValidationException {

		start = DateUtils.convertFromTimezoneToUtc(start, getCurrentSite().getTimeZone());
		end = DateUtils.convertFromTimezoneToUtc(end, getCurrentSite().getTimeZone());
		if (start.after(end)) {
			throw new FieldValidationException("Start has to be before end");
		}
		long interval = end.getTime()-start.getTime();
		if (interval > Config.LIGHTNING_HISTORY_MAX_INTERVAL*24*3600*1000) {
			throw new FieldValidationException("Interval for lightning potential history is maximum "  + Config.LIGHTNING_HISTORY_MAX_INTERVAL + " days");
		}
		
		List<LightningHistoryChartDTO> list = dataSourceManager.getLightningHistory(start, end, alarm1, alarm2, sensorId, getCurrentSite().getId());

		return new ListLoadResultBean<LightningHistoryChartDTO>(list);
	}

	@Override
	public List<TIHTable> getTihTable() {
		return ErgTihImport.parseTIHTable();
	}
	
	@Override
	public String createLightningPotentialHistoryReport(Date start, Date end, Double alarm1, Double alarm2, Integer sensorId, String base64ChartData)  throws FieldValidationException {
		if(start==null || end==null || alarm1 == null || alarm2==null ){
			throw new RuntimeException("Invalid data");
		}
		
		try {
		List<LightningHistoryChartDTO> data = getLightningHistoryData(start,end,alarm1,alarm2, sensorId).getData();
		final SimpleDateFormat refDateSdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		String refDateFormatted = refDateSdf.format(DateUtils.convertToTimezone(new Date(),getCurrentSite().getTimeZone()));
		final String outFileName = "LightningPotentialReport-"+refDateFormatted;
		final String basePath = getBaseFileFolderForUserFiles("report");
		final String reportTemplateOutFilePath = basePath+outFileName+".docx";
		final SiteSettings siteSettings = siteManager.getSiteSettings(getCurrentSite());
		final ReportFileType reportType = siteSettings.getReportFileType();
		String pdfReportFileName = null;
		
		String templateName = Config.LIGHTNING_TEMPLATE;
		final String inFilePath = getBaseFileFolderForSiteFiles()+templateName;
		
		InputStream templateFileInputStream = null;
		if(Files.isRegularFile(Paths.get(inFilePath))){
			templateFileInputStream = new FileInputStream(inFilePath);
		}else{
			templateFileInputStream = lightningTemplateResource.getInputStream();
		}
			
		OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(new File(reportTemplateOutFilePath)));
		InputStream in = new BufferedInputStream(templateFileInputStream);
			
		IXDocReport report = XDocReportRegistry.getRegistry().loadReport(in, TemplateEngineKind.Freemarker);
		FreemarkerTemplateEngine engine = ((FreemarkerTemplateEngine)report.getTemplateEngine());
		engine.getFreemarkerConfiguration().setTemplateExceptionHandler(new IgnoreNullsTemplateExceptionHandler());
			
		FieldsMetadata metadata = report.createFieldsMetadata();
		metadata.addFieldAsImage("ChartImage");
		
		metadata.addFieldAsList("data.duration");
		metadata.addFieldAsList("data.potential");
		metadata.addFieldAsList("data.percent");
		
		report.setFieldsMetadata(metadata);
		
		IContext context = report.createContext();
		
		if(base64ChartData!=null && base64ChartData.length()>0){
			IImageProvider image = new ByteArrayImageProvider(Base64.getDecoder().decode( base64ChartData.substring(base64ChartData.indexOf(',')+1)), true);
			image.setUseImageSize(true);
			image.setResize(true);
			context.put("ChartImage", image);
		}
		
		context.put("from", start);
		context.put("to", end);
		context.put("data", data);
		context.put("site", getCurrentSite());
		context.put("user", getCurrentUser());
		context.put("sensor", dataSourceManager.getSensor(sensorId));
		context.put("dateTime",DateUtils.convertToTimezone(new Date(), getCurrentSite().getTimeZone()));
		
		BeansWrapper wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();
		TemplateHashModel staticModels = wrapper.getStaticModels();
		
		TemplateHashModel formatUtils = (TemplateHashModel) staticModels.get("com.safer.one.gwt.shared.FormatUtils");  
		context.put("FormatUtils", formatUtils);

		report.process(context, fileOut);
		fileOut.flush();
		fileOut.close();
		in.close();
		
		if(ReportFileType.PDF.equals(reportType)){
			pdfReportFileName = pdfConverter.convertWordToPDF(reportTemplateOutFilePath, basePath);
		}
		String fileName = outFileName + (pdfReportFileName==null?".docx":".pdf");
		
		return properties.getApplicationUrl() + Config.DOWNLOAD_FILE_SERVICE_PATH+"?action=downloadFile&downloadType=report&fileName=" + fileName;
		}catch( Exception  e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public ListLoadResult<LightningStrikesChartDTO> getLightningStrikesHistoryData(Date start, Date end,
			Integer sensorId) {
		if (sensorId == null || start == null || end == null || end.before(start)){
			throw new RuntimeException("Invalid data");
		}

		start = DateUtils.getBeginnigOfTheDay(start);
		end = DateUtils.getEndOfTheDay(end);
		if (start.after(end)) {
			throw new RuntimeException("Start can't be after end");
		}
		
		Date utcStart = DateUtils.convertFromTimezoneToUtc(start, getCurrentSite().getTimeZone());
		Date utcEnd = DateUtils.convertFromTimezoneToUtc(end, getCurrentSite().getTimeZone());
		
		LightningAverage total = dataSourceManager.getLightningStrikesTotal(utcStart, utcEnd, sensorId);
		
		ArrayList<LightningStrikesChartDTO> strikesCount = new ArrayList<LightningStrikesChartDTO>();
		
		String[] labels = getLightningStrikesChartLabels();
		strikesCount.add(new LightningStrikesChartDTO(1, total.getStrikes1(), labels[0]));
		strikesCount.add(new LightningStrikesChartDTO(2, total.getStrikes2(), labels[1]));
		strikesCount.add(new LightningStrikesChartDTO(3, total.getStrikes3(), labels[2]));
		return new ListLoadResultBean<LightningStrikesChartDTO>(strikesCount);
	}

	private String[] getLightningStrikesChartLabels() {
		Units units = unitsManager.getUnits(getCurrentSite().getId(),getCurrentUser().getId());
		boolean isMetric = units.getUnitDesc(UnitParam.IDP_INTERFACE_DISTANCES_LENGTH) == UnitDesc.m_km;
		String um = isMetric ? "km":"mi";
		
		return new String[]{"under " + (isMetric ? FormatUtils.getFormatted(units.convert(UnitDesc.mile, UnitDesc.km, 5d), 1) : "5") + " " + um,
			"under " + (isMetric ? FormatUtils.getFormatted(units.convert(UnitDesc.mile, UnitDesc.km, 10d), 1) : "10") + " " + um, 
			
			"under " + (isMetric ? FormatUtils.getFormatted(units.convert(UnitDesc.mile, UnitDesc.km, 20d), 1) : "20") + " " + um};
	}

	@Override
	public String createLightningStikesHistoryReport(Date start, Date end, Integer sensorId, String base64ChartReport) {
		if(start==null || end==null || end.before(start) || sensorId == null){
			throw new RuntimeException("Invalid data");
		}
		
		try {
			
			Sensor sensor = dataSourceManager.getSensor(sensorId);
			if (sensor == null) {
				throw new RuntimeException("Invalid sensor");
			}
			
			start = DateUtils.getBeginnigOfTheDay(start);
			end = DateUtils.getEndOfTheDay(end);
			if (start.after(end)) {
				throw new RuntimeException("Start can't be after end");
			}
			Date utcStart = DateUtils.convertFromTimezoneToUtc(start, getCurrentSite().getTimeZone());
			Date utcEnd = DateUtils.convertFromTimezoneToUtc(end, getCurrentSite().getTimeZone());
	
			List<LightningAverage> data = dataSourceManager.getDailyLightningStrikes(utcStart, utcEnd, sensor);
	
			final SimpleDateFormat refDateSdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
			String refDateFormatted = refDateSdf.format(DateUtils.convertToTimezone(new Date(),getCurrentSite().getTimeZone()));
			final String outFileName = "LightningStrikesReport-"+refDateFormatted;
			final String basePath = getBaseFileFolderForUserFiles("report");
			final String reportTemplateOutFilePath = basePath+outFileName+".docx";
			final SiteSettings siteSettings = siteManager.getSiteSettings(getCurrentSite());
			final ReportFileType reportType = siteSettings.getReportFileType();
			String pdfReportFileName = null;
			
			String templateName = Config.STRIKES_TEMPLATE;
			final String inFilePath = getBaseFileFolderForSiteFiles()+templateName;
			
			InputStream templateFileInputStream = null;
			if(Files.isRegularFile(Paths.get(inFilePath))){
				templateFileInputStream = new FileInputStream(inFilePath);
			}else{
				templateFileInputStream = lightningStrikesTemplateResource.getInputStream();
			}
				
			OutputStream fileOut = new BufferedOutputStream(new FileOutputStream(new File(reportTemplateOutFilePath)));
			InputStream in = new BufferedInputStream(templateFileInputStream);
				
			IXDocReport report = XDocReportRegistry.getRegistry().loadReport(in, TemplateEngineKind.Freemarker);
			FreemarkerTemplateEngine engine = ((FreemarkerTemplateEngine)report.getTemplateEngine());
			engine.getFreemarkerConfiguration().setTemplateExceptionHandler(new IgnoreNullsTemplateExceptionHandler());
				
			FieldsMetadata metadata = report.createFieldsMetadata();
			metadata.addFieldAsImage("ChartImage");
			
			metadata.addFieldAsList("data.strikes1");
			metadata.addFieldAsList("data.strikes2");
			metadata.addFieldAsList("data.strikes3");
			metadata.addFieldAsList("data.strikes");
			metadata.addFieldAsList("data.dateTaken");
			
			report.setFieldsMetadata(metadata);
			
			IContext context = report.createContext();
			
			if(base64ChartReport!=null && base64ChartReport.length()>0){
				IImageProvider image = new ByteArrayImageProvider(Base64.getDecoder().decode( base64ChartReport.substring(base64ChartReport.indexOf(',')+1)), true);
				image.setUseImageSize(true);
				image.setResize(true);
				context.put("ChartImage", image);
			}
			
			double total1 = 0, total2 = 0, total3 = 0, total = 0;
			for (LightningAverage avg:data) {
				total1 += avg.getStrikes1();
				total2 += avg.getStrikes2();
				total3 += avg.getStrikes3();
				total += avg.getStrikes();
			}
			
			String[] labels = getLightningStrikesChartLabels();
			
			context.put("label1", labels[0]);
			context.put("label2", labels[1]);
			context.put("label3", labels[2]);
			context.put("totalStrikes", total);
			context.put("totalStrikes1", total1);
			context.put("totalStrikes2", total2);
			context.put("totalStrikes3", total3);
			context.put("from", start);
			context.put("to", end);
			context.put("data", data);
			context.put("site", getCurrentSite());
			context.put("user", getCurrentUser());
			context.put("sensor", sensor);
			context.put("dateTime",DateUtils.convertToTimezone(new Date(), getCurrentSite().getTimeZone()));
			
			BeansWrapper wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_21).build();
			TemplateHashModel staticModels = wrapper.getStaticModels();
			
			TemplateHashModel formatUtils = (TemplateHashModel) staticModels.get("com.safer.one.gwt.shared.FormatUtils");  
			context.put("FormatUtils", formatUtils);
	
			report.process(context, fileOut);
			fileOut.flush();
			fileOut.close();
			in.close();
			
			if(ReportFileType.PDF.equals(reportType)){
				pdfReportFileName = pdfConverter.convertWordToPDF(reportTemplateOutFilePath, basePath);
			}
			String fileName = outFileName + (pdfReportFileName==null?".docx":".pdf");
			
			return properties.getApplicationUrl() + Config.DOWNLOAD_FILE_SERVICE_PATH+"?action=downloadFile&downloadType=report&fileName=" + fileName;
		}catch( Exception  e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}
