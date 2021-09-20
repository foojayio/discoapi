/*
 * Copyright (c) 2021.
 *
 * This file is part of DiscoAPI.
 *
 *     DiscoAPI is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     DiscoAPI is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DiscoAPI.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.foojay.api;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import io.foojay.api.pkg.Distro;
import io.foojay.api.pkg.Pkg;
import io.foojay.api.util.Config;
import io.foojay.api.util.Constants;
import io.foojay.api.util.EphemeralIdCache;
import io.foojay.api.util.Helper;
import io.foojay.api.util.OutputFormat;
import io.foojay.api.util.State;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.inc;
import static com.mongodb.client.model.Updates.set;
import static io.foojay.api.pkg.Pkg.FIELD_DISTRIBUTION;
import static io.foojay.api.pkg.Pkg.FIELD_FILENAME;
import static io.foojay.api.pkg.Pkg.FIELD_LATEST_BUILD_AVAILABLE;
import static io.foojay.api.util.Constants.API_VERSION_V1;
import static io.foojay.api.util.Constants.COMMA;
import static io.foojay.api.util.Constants.COMMA_NEW_LINE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.CURLY_BRACKET_OPEN;
import static io.foojay.api.util.Constants.SENTINEL_PKG_ID;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_CLOSE;
import static io.foojay.api.util.Constants.SQUARE_BRACKET_OPEN;


public enum MongoDbManager {
    INSTANCE;

    private static final Logger                           LOGGER                         = LoggerFactory.getLogger(MongoDbManager.class);
    private static final String                           FIELD_PACKAGE_ID               = "id";
    private static final String                           FIELD_EPHEMERAL_ID             = "ephemeral_id";
    private static final String                           FIELD_DOWNLOADS                = "downloads";
    private static final String                           FIELD_ID                       = "id";
    private static final String                           FIELD_DISTRO                   = "distro";
    private static final String                           FIELD_DISTRIBUTIONS            = "distributions";
    private static final String                           FIELD_VERSION                  = "version";
    private static final String                           FIELD_DAY                      = "day";
    private static final String                           FIELD_TIMESTAMP                = "timestamp";
    private static final String                           FIELD_STATE                    = "state";
    private static final String                           FIELD_TYPE                     = "type";
    private static final String                           FIELD_REMOVED_AT               = "removedat";
    private static final String                           FIELD_USER_AGENT               = "useragent";
    private static final String                           FIELD_COUNTRY_CODE             = "countrycode";
    private static final String                           FIELD_LAST_EPHEMERAL_ID_UPDATE = "lastephemeralidupdate";
    public final         EphemeralIdCache<String, String> ephemeralIdCache               = new EphemeralIdCache<>();
    private              MongoClient                      mongoClient;
    private              boolean                          connected;
    private              MongoDatabase                    database;


    MongoDbManager() {
        connected = false;
        // Set mongodb logger to SEVERE only.
        java.util.logging.Logger mongoLogger = java.util.logging.Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.SEVERE);
    }


    public void init() {
        if (null == Config.INSTANCE.getFoojayMongoDbUser() ||
            null == Config.INSTANCE.getFoojayMongoDbPassword() ||
            null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            connected = false;
        } else {
            try {
                final MongoCredential credential = MongoCredential.createCredential(Config.INSTANCE.getFoojayMongoDbUser(),
                                                                                    Config.INSTANCE.getFoojayMongoDbDatabase(),
                                                                                    Config.INSTANCE.getFoojayMongoDbPassword().toCharArray());
                mongoClient = MongoClients.create(MongoClientSettings.builder()
                                                                     .applyToClusterSettings(builder -> builder.hosts(Arrays.asList(new ServerAddress(Config.INSTANCE.getFoojayMongoDbUrl(), Config.INSTANCE.getFoojayMongoDbPort()))))
                                                                     .credential(credential)
                                                                     .build());

                database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
                connected = true;
                LOGGER.debug("Established connection to mongodb at {}:{}", Config.INSTANCE.getFoojayMongoDbUrl(), Config.INSTANCE.getFoojayMongoDbPort());

                if (!collectionExists(database, Constants.STATE_COLLECTION)) { database.createCollection(Constants.STATE_COLLECTION); }
                if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }
                if (!collectionExists(database, Constants.EPHEMERAL_IDS_COLLECTION)) { database.createCollection(Constants.EPHEMERAL_IDS_COLLECTION); }
                if (!collectionExists(database, Constants.DOWNLOADS_COLLECTION)) { database.createCollection(Constants.DOWNLOADS_COLLECTION); }
                if (!collectionExists(database, Constants.DOWNLOADS_USER_AGENT_COLLECTION)) { database.createCollection(Constants.DOWNLOADS_USER_AGENT_COLLECTION); }
                if (!collectionExists(database, Constants.DISTRO_UPDATES_COLLECTION)) { database.createCollection(Constants.DISTRO_UPDATES_COLLECTION); }
                if (!collectionExists(database, Constants.SHEDLOCK_COLLECTION)) { database.createCollection(Constants.SHEDLOCK_COLLECTION); }

                updateEphemeralIds();
                setState(State.IDLE);
            } catch (MongoException e) {
                connected = false;
                LOGGER.debug("Error connecting to mongodb at {}:{}. {}", Config.INSTANCE.getFoojayMongoDbUrl(), Config.INSTANCE.getFoojayMongoDbPort(), e.getMessage());
            }
        }
    }

    public boolean isConnected() { return connected; }

    public void connect() {
        if (connected) { return; }
        init();
    }

    public MongoDatabase getDatabase() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected");
            return null;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return null;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        return database;
    }

    public State getState() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, returned idle state");
            return State.IDLE;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Cannot return state because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return State.IDLE;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.STATE_COLLECTION) {
            LOGGER.error("Constants.STATE_COLLECTION not set.");
            return State.IDLE;
        };
        if (!collectionExists(database, Constants.STATE_COLLECTION)) { database.createCollection(Constants.STATE_COLLECTION); }

        final Instant                   now        = Instant.now();
        final MongoCollection<Document> collection = database.getCollection(Constants.STATE_COLLECTION);
        Document document = collection.find(eq(FIELD_TYPE, FIELD_STATE)).first();
        if (null == document) {
            collection.updateOne(eq(FIELD_TYPE, FIELD_STATE), combine(set(FIELD_TYPE, FIELD_STATE), set(FIELD_STATE, State.IDLE.name()), set(FIELD_TIMESTAMP, now.getEpochSecond())), new UpdateOptions().upsert(true));
            return State.IDLE;
        } else {
            final State   state     = State.valueOf(document.getString(FIELD_STATE));
            final Instant timestamp = Instant.ofEpochSecond(document.getLong(FIELD_TIMESTAMP));
            final long    minutes   = Duration.between(timestamp, Instant.now()).toMinutes();
            switch(state) {
                case UPDATING:
                    if (minutes > Constants.UPDATE_TIMEOUT_IN_MINUTES) {
                        collection.updateOne(eq(FIELD_TYPE, FIELD_STATE), combine(set(FIELD_TYPE, FIELD_STATE), set(FIELD_STATE, State.IDLE.name()), set(FIELD_TIMESTAMP, now.getEpochSecond())), new UpdateOptions().upsert(true));
                        return State.IDLE;
                    } else {
                        return state;
                    }
                case PRELOADING:
                    if (minutes > Constants.PRELOAD_TIMEOUT_IN_MINUTES) {
                        collection.updateOne(eq(FIELD_TYPE, FIELD_STATE), combine(set(FIELD_TYPE, FIELD_STATE), set(FIELD_STATE, State.IDLE.name()), set(FIELD_TIMESTAMP, now.getEpochSecond())), new UpdateOptions().upsert(true));
                        return State.IDLE;
                    } else {
                        return state;
                    }
                case UPLOADING:
                    if (minutes > Constants.UPLOAD_TIMEOUT_IN_MINUTES) {
                        collection.updateOne(eq(FIELD_TYPE, FIELD_STATE), combine(set(FIELD_TYPE, FIELD_STATE), set(FIELD_STATE, State.IDLE.name()), set(FIELD_TIMESTAMP, now.getEpochSecond())), new UpdateOptions().upsert(true));
                        return State.IDLE;
                    } else {
                        return state;
                    }
                case SYNCRONIZING:
                    if (minutes > Constants.SYNCHRONIZING_TIMEOUT_IN_MINUTES) {
                        collection.updateOne(eq(FIELD_TYPE, FIELD_STATE), combine(set(FIELD_TYPE, FIELD_STATE), set(FIELD_STATE, State.IDLE.name()), set(FIELD_TIMESTAMP, now.getEpochSecond())), new UpdateOptions().upsert(true));
                        return State.IDLE;
                    } else {
                        return state;
                    }
                case IDLE:
                default  : return state;
            }
        }
    }
    public String getStateJson() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, returned idle state");
            return null;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Cannot return state because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return null;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.STATE_COLLECTION) {
            LOGGER.error("Constants.STATE_COLLECTION not set.");
            return null;
        };

        if (!collectionExists(database, Constants.STATE_COLLECTION)) {
            database.createCollection(Constants.STATE_COLLECTION);
        }

        final MongoCollection<Document> collection = database.getCollection(Constants.STATE_COLLECTION);
        Document document = collection.find(eq(FIELD_TYPE, FIELD_STATE)).first();
        if (null == document) {
            return null;
        } else {
            return document.toJson();
        }
    }
    public void setState(final State state) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, state not set");
            return;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Cannot set state because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.STATE_COLLECTION) {
            LOGGER.error("Constants.STATE_COLLECTION not set.");
            return;
        };
        if (!collectionExists(database, Constants.STATE_COLLECTION)) { database.createCollection(Constants.STATE_COLLECTION); }
        database.getCollection(Constants.STATE_COLLECTION)
                .updateOne(eq(FIELD_TYPE, FIELD_STATE), combine(set(FIELD_TYPE, FIELD_STATE), set(FIELD_STATE, state.name()), set(FIELD_TIMESTAMP, Instant.now().getEpochSecond())), new UpdateOptions().upsert(true));
    }

    /**
     * Returns list of all packages in the packages collection
     * @return list of all packages in the packages collection
     */
    public List<Pkg> getPkgs() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, returned empty list of packages");
            return new ArrayList<>();
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Cannot return packages because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return new ArrayList<>();
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.BUNDLES_COLLECTION not set.");
            return new ArrayList<>();
        };
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }

        final MongoCollection<Document> collection = database.getCollection(Constants.PACKAGES_COLLECTION);
        final List<Pkg>                 result     = new ArrayList<>();
        collection.find().forEach(document -> {
            Pkg pkg = new Pkg(document.toJson());
            result.add(pkg);
        });
        LOGGER.debug("Successfully returned {} packages from mongodb.", result.size());
        return result;
    }

    public List<Pkg> getPkgsForDistro(final Distro distro) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, returned empty list of packages");
            return new ArrayList<>();
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Cannot return packages because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return new ArrayList<>();
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.BUNDLES_COLLECTION not set.");
            return new ArrayList<>();
        };
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }

        final MongoCollection<Document> collection = database.getCollection(Constants.PACKAGES_COLLECTION);
        final List<Pkg>                 result     = new ArrayList<>();
        collection.find(eq(FIELD_DISTRIBUTION, distro.getApiString()))
                  .forEach(document -> {
            Pkg pkg = new Pkg(document.toJson());
            result.add(pkg);
        });
        LOGGER.debug("Successfully returned {} packages for distribution {} from mongodb.", result.size(), distro.name());
        return result;
    }

    /**
     * Inserts given list of packages to packages collection
     * @param pkgs
     */
    public void insertAllPkgs(final Collection<Pkg> pkgs) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, no packages inserted");
            return;
        }
        if (null == pkgs || pkgs.isEmpty()) {
            LOGGER.debug("Packages are null or empty.");
            return;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Packages not inserted because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.BUNDLES_COLLECTION not set.");
            return;
        };
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }

        final MongoCollection<Document> collection = database.getCollection(Constants.PACKAGES_COLLECTION);
        final List<Document>            documents  = new ArrayList<>();
        for (Pkg pkg : pkgs) {
            try {
                long count = collection.countDocuments(new BsonDocument(FIELD_PACKAGE_ID, new BsonString(pkg.getId())));
                if (count == 0) { documents.add(Document.parse(pkg.toString(OutputFormat.FULL_COMPRESSED, API_VERSION_V1))); }
            } catch (JsonParseException e) {
                LOGGER.error("Error parsing json when adding package {}. {}", pkg.getId(), e.getMessage());
            }
        }
        collection.insertMany(documents);
        LOGGER.debug("Successfully inserted {} packages to mongodb.", pkgs.size());
    }

    /**
     * Adds the given list of packages to the packages collection where existing packages will be updated
     * @param pkgs
     * @return true when packages have been added successfully
     */
    public boolean addNewPkgs(final Collection<Pkg> pkgs) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, no packages added");
            return false;
        }
        if (null == pkgs || pkgs.isEmpty()) {
            LOGGER.debug("Packages are null or empty.");
            return false;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("New packages not added because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return false;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.PACKAGES_COLLECTION not set.");
            return false;
        };
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }

        MongoCollection<Document> collection = database.getCollection(Constants.PACKAGES_COLLECTION);
        ReplaceOptions replaceOptions = new ReplaceOptions().upsert(true);
        for (Pkg pkg : pkgs) {
            try {
                Document document = Document.parse(pkg.toString(OutputFormat.FULL_COMPRESSED, API_VERSION_V1));
                collection.replaceOne(eq(FIELD_PACKAGE_ID, pkg.getId()), document, replaceOptions);
            } catch (JsonParseException e) {
                LOGGER.error("Error parsing json when adding package {}. {}", pkg.getId(), e.getMessage());
            }
        }
        LOGGER.debug("Successfully added {} new packages to mongodb.", pkgs.size());
        return true;
    }

    /**
     * Removes the given list of packages from the packages collection
     * @param pkgs
     * @return true when packages have been removed successfully
     */
    public boolean removePkgs(final Collection<Pkg> pkgs) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, no packages have been removed");
            return false;
        }
        if (null == pkgs || pkgs.isEmpty()) {
            LOGGER.debug("Packages are null or empty.");
            return false;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Packages have not been removed because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return false;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.PACKAGES_COLLECTION not set.");
            return false;
        };
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }

        MongoCollection<Document> collection = database.getCollection(Constants.PACKAGES_COLLECTION);
        for (Pkg pkg : pkgs) {
            try {
                Bson deleteFilter = eq(FIELD_FILENAME, pkg.getFileName());
                collection.deleteOne(deleteFilter);
            } catch (JsonParseException e) {
                LOGGER.error("Error when deleting package {}. {}", pkg.getId(), e.getMessage());
            }
        }
        LOGGER.debug("Successfully deleted {} packages from mongodb.", pkgs.size());
        return true;
    }

    /**
     * Removes all documents from the packages collection.
     * This method can be called from Updater.runOnceAtStartup() to wipe all documents
     * in case a new cache warmup file is provided that contains additional information
     * @return true when all documents have been removed successfully
     */
    public boolean removeAllPkgs() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, no packages have been removed");
            return false;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Packages have not been removed because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return false;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.PACKAGES_COLLECTION not set.");
            return false;
        };
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }

        MongoCollection<Document> collection = database.getCollection(Constants.PACKAGES_COLLECTION);
        collection.deleteMany(new Document());

        LOGGER.debug("Successfully deleted all packages from mongodb.");
        return true;
    }

    /**
     * Returns a map with the packageId as key and the number of downloads as value.
     * With this one can determine which are most loaded packages.
     * @return a map with the packageId as key and the number of downloads as value
     */
    public Map<String, Long> getDowloads() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, return empty map of downloads");
            return new HashMap<>();
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Cannot return downloads because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return new HashMap<>();
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.DOWNLOADS_COLLECTION) {
            LOGGER.error("Constants.DOWNLOADS_COLLECTION not set.");
            return new HashMap<>();
        };
        if (!collectionExists(database, Constants.DOWNLOADS_COLLECTION)) { database.createCollection(Constants.DOWNLOADS_COLLECTION); }

        final Map<String, Long>  downloads        = new ConcurrentHashMap<>();
        final Consumer<Document> downloadConsumer = document -> downloads.put(document.getString(FIELD_PACKAGE_ID), document.getLong(FIELD_DOWNLOADS));

        final MongoCollection<Document> collection = database.getCollection(Constants.DOWNLOADS_COLLECTION);
        collection.find().forEach(downloadConsumer);

        LOGGER.debug("Successfully restored downloads for {} package ids from mongodb.", downloads.size());
        return downloads;
    }

    /**
     * Returns a json string that contains all entries within the given range of timestamps
     * @param from epoch seconds from where to include
     * @param to epoch seconds to where
     * @return a json string that contains all entries within the given range of timestamps
     */
    public String getPkgDownloadsInclUserAgent(final Long from, final Long to) {
        Long start = null == from ? Instant.MIN.getEpochSecond() : from;
        Long end   = null == to   ? Instant.MAX.getEpochSecond() : to;
        if (null != from && null != to) {
            if (from > to) { start = Instant.MIN.getEpochSecond(); }
            if (to < from) { end = Instant.MAX.getEpochSecond(); }
        }

        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, return empty map of downloads");
            return CURLY_BRACKET_OPEN + CURLY_BRACKET_CLOSE;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Cannot return downloads because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return CURLY_BRACKET_OPEN + CURLY_BRACKET_CLOSE;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.DOWNLOADS_COLLECTION) {
            LOGGER.error("Constants.DOWNLOADS_USER_AGENT_COLLECTION not set.");
            return CURLY_BRACKET_OPEN + CURLY_BRACKET_CLOSE;
        };
        if (!collectionExists(database, Constants.DOWNLOADS_USER_AGENT_COLLECTION)) { database.createCollection(Constants.DOWNLOADS_USER_AGENT_COLLECTION); }

        final MongoCollection<Document> collection = database.getCollection(Constants.DOWNLOADS_USER_AGENT_COLLECTION);

        final StringBuilder      msgBuilder       = new StringBuilder();
        final Consumer<Document> downloadConsumer = document -> msgBuilder.append(document.toJson()).append(COMMA_NEW_LINE);

        collection.find(and(gte(FIELD_TIMESTAMP, start), lte(FIELD_TIMESTAMP, end))).forEach(downloadConsumer);
        if (msgBuilder.length() > 2) {
            msgBuilder.setLength(msgBuilder.length() - 2);
        }

        return msgBuilder.toString();
    }

    /**
     * Update number of downloads for given packageId
     * @param pkgId
     * @param noOfDownloads
     */
    public void upsertDownloadForId(final String pkgId, final Long noOfDownloads) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, no packages updated");
            return;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Could not upsert download because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.DOWNLOADS_COLLECTION) {
            LOGGER.error("Constants.DOWNLOADS_COLLECTION not set.");
            return;
        }
        if (!collectionExists(database, Constants.DOWNLOADS_COLLECTION)) { database.createCollection(Constants.DOWNLOADS_COLLECTION); }

        database.getCollection(Constants.DOWNLOADS_COLLECTION)
                .updateOne(eq(FIELD_PACKAGE_ID, pkgId), combine(set(FIELD_PACKAGE_ID, pkgId), set(FIELD_DOWNLOADS, noOfDownloads)), new UpdateOptions().upsert(true));

        LOGGER.debug("Successfully updated no of downloads for id {}", pkgId);
    }

    public void addDownloadFromUserAgent(final String pkgId, final String userAgent, final String ipAddress) {
        String countryCode = Helper.getCountryCode(ipAddress);
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, no packages updated");
            return;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Could not upsert download because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.DOWNLOADS_USER_AGENT_COLLECTION) {
            LOGGER.error("Constants.DOWNLOADS_USER_AGENT_COLLECTION not set.");
            return;
        }
        if (!collectionExists(database, Constants.DOWNLOADS_USER_AGENT_COLLECTION)) { database.createCollection(Constants.DOWNLOADS_USER_AGENT_COLLECTION); }

        Document document = new Document();
        document.append(FIELD_PACKAGE_ID, pkgId);
        document.append(FIELD_USER_AGENT, userAgent);
        document.append(FIELD_COUNTRY_CODE, countryCode);
        document.append(FIELD_TIMESTAMP, Instant.now().getEpochSecond());

        database.getCollection(Constants.DOWNLOADS_USER_AGENT_COLLECTION).insertOne(document);

        LOGGER.debug("Successfully added download for id {} and user-agent {}", pkgId, userAgent);
    }

    public void addDownloadToToday(final Distro distro, final int majorVersion) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, downloads per distro not set.");
            return;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Could not upsert update because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.DOWNLOADS_PER_DAY_COLLECTION) {
            LOGGER.error("Constants.DOWNLOADS_PER_DAY_COLLECTION not set.");
            return;
        }
        if (!collectionExists(database, Constants.DOWNLOADS_PER_DAY_COLLECTION)) { database.createCollection(Constants.DOWNLOADS_PER_DAY_COLLECTION); }

        final String featureVersion = Integer.toString(majorVersion);
        final String day            = DateTimeFormatter.ISO_LOCAL_DATE.format(ZonedDateTime.now());

        final MongoCollection<Document> collection = database.getCollection(Constants.DOWNLOADS_PER_DAY_COLLECTION);

        Document dayDoc = collection.find(eq(FIELD_DAY, day)).first();
        if (null == dayDoc) {
            dayDoc = new Document();
        }

        Document distributionsDoc;
        if (dayDoc.containsKey(FIELD_DISTRIBUTIONS)) {
            distributionsDoc = (Document) dayDoc.get(FIELD_DISTRIBUTIONS);
            Document distroDoc;
            long     numberOfAllDownloads = 0;
            Document versionsDoc;
            long     downloadsPerVersion = 0;
            if (distributionsDoc.containsKey(distro.getApiString())) {
                distroDoc = (Document) distributionsDoc.get(distro.getApiString());
                if (distroDoc.containsKey(FIELD_DOWNLOADS)) {
                    numberOfAllDownloads = distroDoc.getLong(FIELD_DOWNLOADS);
                }
                if (distroDoc.containsKey(FIELD_VERSION)) {
                    versionsDoc = (Document) distroDoc.get(FIELD_VERSION);
                    if (versionsDoc.containsKey(featureVersion)) {
                        downloadsPerVersion = versionsDoc.getLong(featureVersion);
                    }
                } else {
                    versionsDoc = new Document();
                }
                versionsDoc.put(featureVersion, downloadsPerVersion + 1);
            } else {
                versionsDoc = new Document();
                versionsDoc.put(featureVersion, downloadsPerVersion + 1);
                distroDoc = new Document();
            }
            distroDoc.put(FIELD_DOWNLOADS, numberOfAllDownloads + 1);
            distroDoc.put(FIELD_VERSION, versionsDoc);

            distributionsDoc.append(distro.getApiString(), distroDoc);
        } else {
            Document versions = new Document();
            versions.put(featureVersion, 1);

            Document distroDoc = new Document();
            distroDoc.put(FIELD_DOWNLOADS, 1);
            distroDoc.put(FIELD_VERSION, versions);

            distributionsDoc = new Document();
            distributionsDoc.put(distro.getApiString(), distroDoc);
        }
        dayDoc.put(day, distributionsDoc);

        collection.updateOne(eq(FIELD_DAY, day), combine(set(FIELD_DISTRIBUTIONS, distributionsDoc)), new UpdateOptions().upsert(true));

        LOGGER.debug("Successfully updated downloads for distro {} at {}", distro.getApiString(), day);
    }
    public String getDownloadsPerDay(final Set<ZonedDateTime> days) {
        if (days.isEmpty()) {
            days.add(ZonedDateTime.of(2021, 9, 6, 12, 0, 0, 0, ZoneId.systemDefault()));
            days.add(ZonedDateTime.now());
        }
        List<String> daysToFetch = days.stream().map(day -> DateTimeFormatter.ISO_LOCAL_DATE.format(day)).collect(Collectors.toList());

        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, returned empty list of downloads");
            return SQUARE_BRACKET_OPEN + SQUARE_BRACKET_CLOSE;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Cannot return packages because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return SQUARE_BRACKET_OPEN + SQUARE_BRACKET_CLOSE;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.DOWNLOADS_PER_DAY_COLLECTION) {
            LOGGER.error("Constants.DOWNLOADS_PER_DAY_COLLECTION not set.");
            return SQUARE_BRACKET_OPEN + SQUARE_BRACKET_CLOSE;
        };
        if (!collectionExists(database, Constants.DOWNLOADS_PER_DAY_COLLECTION)) { database.createCollection(Constants.DOWNLOADS_PER_DAY_COLLECTION); }

        final MongoCollection<Document> collection = database.getCollection(Constants.DOWNLOADS_PER_DAY_COLLECTION);

        StringBuilder msgBuilder = new StringBuilder();
        msgBuilder.append(SQUARE_BRACKET_OPEN);
        collection.find(in(FIELD_DAY, daysToFetch)).forEach(document -> {
            msgBuilder.append(document.toJson());
            msgBuilder.append(COMMA);
        });
        msgBuilder.append(SQUARE_BRACKET_CLOSE);
        return msgBuilder.toString();
    }

    public void updateLatestBuildAvailable(final List<Pkg> pkgs) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, no packages updated");
            return;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Could not update latest build available because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.PACKAGES_COLLECTION not set.");
            return;
        }
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }

        MongoCollection<Document> collection = database.getCollection(Constants.PACKAGES_COLLECTION);
        pkgs.forEach(pkg -> collection.updateOne(eq(FIELD_PACKAGE_ID, pkg.getId()), set(FIELD_LATEST_BUILD_AVAILABLE, false)));

        LOGGER.debug("Successfully updated latest build available for {} packages", pkgs.size());
    }

    public Instant getLastUpdateForDistro(final Distro distro) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, returned empty list of packages");
            return Instant.ofEpochSecond(0);
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Cannot return packages because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return Instant.ofEpochSecond(0);
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.DISTRO_UPDATES_COLLECTION) {
            LOGGER.error("Constants.DISTRO_UPDATES_COLLECTION not set.");
            return Instant.ofEpochSecond(0);
        };
        if (!collectionExists(database, Constants.DISTRO_UPDATES_COLLECTION)) { database.createCollection(Constants.DISTRO_UPDATES_COLLECTION); }

        final MongoCollection<Document> collection = database.getCollection(Constants.DISTRO_UPDATES_COLLECTION);

        Document document = collection.find(eq(FIELD_DISTRO, distro.getApiString())).first();
        if (null == document) {
            return Instant.ofEpochSecond(0);
        } else {
            return Instant.ofEpochSecond(document.getLong(FIELD_TIMESTAMP));
        }
    }
    public void setLastUpdateForDistro(final Distro distro) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, last update for distro not set.");
            return;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Could not upsert update because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.DISTRO_UPDATES_COLLECTION) {
            LOGGER.error("Constants.DISTRO_UPDATES_COLLECTION not set.");
            return;
        }
        if (!collectionExists(database, Constants.DISTRO_UPDATES_COLLECTION)) { database.createCollection(Constants.DISTRO_UPDATES_COLLECTION); }

        database.getCollection(Constants.DISTRO_UPDATES_COLLECTION)
                .updateOne(eq(FIELD_DISTRO, distro.getApiString()), combine(set(FIELD_TIMESTAMP, Instant.now().getEpochSecond())), new UpdateOptions().upsert(true));

        LOGGER.debug("Successfully updated last update for distro {}", distro.getApiString());
    }
    public boolean removeLastUpdates() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, last update entries have not been removed");
            return false;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Last update entries have not been removed because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return false;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.DISTRO_UPDATES_COLLECTION) {
            LOGGER.error("Constants.DISTRO_UPDATES_COLLECTION not set.");
            return false;
        };
        if (!collectionExists(database, Constants.DISTRO_UPDATES_COLLECTION)) { database.createCollection(Constants.DISTRO_UPDATES_COLLECTION); }

        MongoCollection<Document> collection = database.getCollection(Constants.DISTRO_UPDATES_COLLECTION);
        collection.deleteMany(new Document());

        LOGGER.debug("Successfully deleted all last update entries from mongodb.");
        return true;
    }

    public boolean removeDownloads() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, no locks have been removed");
            return false;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Locks have not been removed because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return false;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.DOWNLOADS_COLLECTION) {
            LOGGER.error("Constants.DOWNLOADS_COLLECTION not set.");
            return false;
        };
        if (!collectionExists(database, Constants.DOWNLOADS_COLLECTION)) { database.createCollection(Constants.DOWNLOADS_COLLECTION); }

        MongoCollection<Document> collection = database.getCollection(Constants.DOWNLOADS_COLLECTION);
        collection.deleteMany(new Document());

        LOGGER.debug("Successfully deleted all downloads from mongodb.");
        return true;
    }

    public boolean removeDownloadsPerPkg() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, no locks have been removed");
            return false;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Locks have not been removed because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return false;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.DOWNLOADS_USER_AGENT_COLLECTION) {
            LOGGER.error("Constants.DOWNLOADS_USER_AGENT_COLLECTION not set.");
            return false;
        };
        if (!collectionExists(database, Constants.DOWNLOADS_USER_AGENT_COLLECTION)) { database.createCollection(Constants.DOWNLOADS_USER_AGENT_COLLECTION); }

        MongoCollection<Document> collection = database.getCollection(Constants.DOWNLOADS_USER_AGENT_COLLECTION);
        collection.deleteMany(new Document());

        LOGGER.debug("Successfully deleted all downloads per pkg from mongodb.");
        return true;
    }

    public boolean removePkgsOfDistro(final Distro distro) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, last update for distro not set.");
            return false;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Could not upsert update because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return false;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.PACKAGES_COLLECTION not set.");
            return false;
        }
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }

        MongoCollection<Document> collection = database.getCollection(Constants.PACKAGES_COLLECTION);
        try {
            Bson deleteFilter = eq(FIELD_DISTRIBUTION, distro.getApiString());
            collection.deleteMany(deleteFilter);
        } catch (JsonParseException e) {
            LOGGER.error("Error when deleting package from {}. {}", distro.getApiString(), e.getMessage());
        }

        LOGGER.debug("Successfully removed packages of distro {}", distro.getApiString());
        return true;
    }

    public void updateEphemeralIds() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, no packages updated");
            return;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Could not update latest build available because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.STATE_COLLECTION) {
            LOGGER.error("Constants.UPDATES_COLLECTION not set.");
            return;
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.PACKAGES_COLLECTION not set.");
            return;
        }
        if (null == Constants.EPHEMERAL_IDS_COLLECTION) {
            LOGGER.error("Constants.EPHEMERAL_IDS_COLLECTION not set.");
            return;
        }

        if (!collectionExists(database, Constants.STATE_COLLECTION))         { database.createCollection(Constants.STATE_COLLECTION); }
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION))      { database.createCollection(Constants.PACKAGES_COLLECTION); }
        if (!collectionExists(database, Constants.EPHEMERAL_IDS_COLLECTION)) { database.createCollection(Constants.EPHEMERAL_IDS_COLLECTION); }

        final long                      start                  = System.currentTimeMillis();
        final MongoCollection<Document> stateCollection        = database.getCollection(Constants.STATE_COLLECTION);
        final MongoCollection<Document> pkgsCollection         = database.getCollection(Constants.PACKAGES_COLLECTION);
        final MongoCollection<Document> ephemeralIdsCollection = database.getCollection(Constants.EPHEMERAL_IDS_COLLECTION);
        final Instant                   now                    = Instant.now();
        final boolean doUpdate;
        // Check for last update of ephemeral ids
        Document lastEphemeralIdUpdateDocument = stateCollection.find(eq(FIELD_TYPE, FIELD_LAST_EPHEMERAL_ID_UPDATE)).first();
        if (null == lastEphemeralIdUpdateDocument) {
            doUpdate = true;
        } else {
            final Instant timestamp = Instant.ofEpochSecond(lastEphemeralIdUpdateDocument.getLong(FIELD_TIMESTAMP));
            doUpdate = (Duration.between(timestamp, now).toMinutes() > 10);
        }

        final Map<String, String> tmpEphemeralIdCache = new HashMap<>();
        if (doUpdate) {
            final List<Document> ephemeralIdDocuments = new ArrayList<>();
            final long epoch = now.getEpochSecond();
            pkgsCollection.find().forEach(document -> {
                final String pkgId       = document.get(FIELD_PACKAGE_ID).toString();
                final String ephemeralId = Helper.createEphemeralId(epoch, pkgId);
                ephemeralIdDocuments.add(new Document().append(FIELD_EPHEMERAL_ID, ephemeralId).append(FIELD_PACKAGE_ID, pkgId));
                tmpEphemeralIdCache.put(ephemeralId, pkgId);
            });
            // Clear ephemeralIdsCollection
            ephemeralIdsCollection.deleteMany(new Document());
            // Insert updated ephemeral ids
            ephemeralIdsCollection.insertMany(ephemeralIdDocuments);

            // Update last ephemeral id update
            stateCollection.updateOne(eq(FIELD_TYPE, FIELD_LAST_EPHEMERAL_ID_UPDATE), combine(set(FIELD_TYPE, FIELD_LAST_EPHEMERAL_ID_UPDATE), set(FIELD_TIMESTAMP, epoch)), new UpdateOptions().upsert(true));
        } else {
            ephemeralIdsCollection.find().forEach(document -> tmpEphemeralIdCache.put(document.getString(FIELD_EPHEMERAL_ID), document.getString(FIELD_PACKAGE_ID)));
        }
        ephemeralIdCache.clear();
        ephemeralIdCache.addAll(tmpEphemeralIdCache);
        LOGGER.debug("Successfully updated ephemeral id cache in {} ms", (System.currentTimeMillis() - start));
    }

    public boolean removeSentinelPackage() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, sentinel not removed.");
            return false;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Cannot remove sentinel because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return false;
        }
        if (null == database) {
            LOGGER.error("Database is not set, sentinel not removed.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.PACKAGES_COLLECTION not set, sentinel not removed.");
            return false;
        };
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }

        final MongoCollection<Document> collection = database.getCollection(Constants.PACKAGES_COLLECTION);
        final Document                  sentinel   = collection.find(eq(FIELD_ID, Constants.SENTINEL_PKG_ID)).first();
        if (null == sentinel) {
            LOGGER.debug("Sentinel not removed from mongodb because it was not found.");
            return false;
        }

        collection.deleteOne(eq(FIELD_ID, Constants.SENTINEL_PKG_ID));
        LOGGER.debug("Sentinel successfully removed from mongodb.");
        CacheManager.INSTANCE.pkgCache.remove(Constants.SENTINEL_PKG_ID);
        LOGGER.debug("Sentinel successfully removed from pkgCache.");

        if (!collectionExists(database, Constants.SENTINEL_COLLECTION)) { database.createCollection(Constants.SENTINEL_COLLECTION); }
        database.getCollection(Constants.SENTINEL_COLLECTION)
                .updateOne(eq(FIELD_ID, Constants.SENTINEL_PKG_ID), set(FIELD_REMOVED_AT, Instant.now().getEpochSecond()), new UpdateOptions().upsert(true));

        return true;
    }
    public boolean isSentinelAvailable() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, cannot get sentinel");
            return false;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Cannot get sentinel because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return false;
        }
        if (null == database) {
            LOGGER.error("Database is not set, cannot get sentinel.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.PACKAGES_COLLECTION not set, cannot get sentinel.");
            return false;
        };
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }

        final MongoCollection<Document> collection = database.getCollection(Constants.PACKAGES_COLLECTION);
        final Document                  sentinel   = collection.find(eq(FIELD_ID, Constants.SENTINEL_PKG_ID)).first();

        if (null == sentinel && CacheManager.INSTANCE.pkgCache.containsKey(SENTINEL_PKG_ID)) {
            Document document = Document.parse(CacheManager.INSTANCE.pkgCache.get(SENTINEL_PKG_ID).toString(OutputFormat.FULL_COMPRESSED, API_VERSION_V1));
            collection.replaceOne(eq(FIELD_PACKAGE_ID, SENTINEL_PKG_ID), document, new ReplaceOptions().upsert(true));
            return true;
        } else {
        return null != sentinel;
    }
    }
    public Instant getSentinelLastRemovedAt() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected.");
            return Instant.ofEpochSecond(0);
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return Instant.ofEpochSecond(0);
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.SENTINEL_COLLECTION) {
            LOGGER.error("Constants.SENTINEL_COLLECTION not set.");
            return Instant.ofEpochSecond(0);
        };
        if (!collectionExists(database, Constants.SENTINEL_COLLECTION)) { database.createCollection(Constants.SENTINEL_COLLECTION); }

        final MongoCollection<Document> collection = database.getCollection(Constants.SENTINEL_COLLECTION);
        final Document                  document   = collection.find(eq(FIELD_ID, Constants.SENTINEL_PKG_ID)).first();
        if (null == document) {
            return Instant.ofEpochSecond(0);
        } else {
            return Instant.ofEpochSecond(document.getLong(FIELD_REMOVED_AT));
        }
    }

    public boolean removeLocks() {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, no locks have been removed");
            return false;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Locks have not been removed because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return false;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.SHEDLOCK_COLLECTION) {
            LOGGER.error("Constants.SHEDLOCK_COLLECTION not set.");
            return false;
        };
        if (!collectionExists(database, Constants.SHEDLOCK_COLLECTION)) { database.createCollection(Constants.SHEDLOCK_COLLECTION); }

        MongoCollection<Document> collection = database.getCollection(Constants.SHEDLOCK_COLLECTION);
        collection.deleteMany(new Document());

        LOGGER.debug("Successfully deleted all locks from mongodb.");
        return true;
    }

    public void syncLatestBuildAvailableInDatabaseWithCache(final Collection<Pkg> pkgs) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not conntected, database was not synced");
            return;
        }
        if (null == Config.INSTANCE.getFoojayMongoDbDatabase()) {
            LOGGER.debug("Could not sync cache with database because FOOJAY_MONGODB_DATABASE environment variable was not set.");
            return;
        }
        if (null == database) {
            LOGGER.error("Database is not set.");
            database = mongoClient.getDatabase(Config.INSTANCE.getFoojayMongoDbDatabase());
        }
        if (null == Constants.PACKAGES_COLLECTION) {
            LOGGER.error("Constants.PACKAGES_COLLECTION not set.");
            return;
        }
        if (!collectionExists(database, Constants.PACKAGES_COLLECTION)) { database.createCollection(Constants.PACKAGES_COLLECTION); }

        MongoCollection<Document> collection = database.getCollection(Constants.PACKAGES_COLLECTION);
        pkgs.forEach(pkg -> collection.updateOne(eq(FIELD_PACKAGE_ID, pkg.getId()), set(FIELD_LATEST_BUILD_AVAILABLE, pkg.isLatestBuildAvailable())));

        LOGGER.debug("Successfully synced latest build available for all packages in cache {}", pkgs.size());
    }

    public boolean collectionExists(final MongoDatabase database, final String collectionName) {
        if (database == null) { return false; }
        final MongoIterable<String> iterable = database.listCollectionNames();
        try (final MongoCursor<String> it = iterable.iterator()) {
            while (it.hasNext()) {
                if (it.next().equalsIgnoreCase(collectionName)) {
                    return true;
                }
            }
        }
        return false;
    }
}
