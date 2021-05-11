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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import static io.foojay.api.pkg.Pkg.FIELD_DISTRIBUTION;
import static io.foojay.api.pkg.Pkg.FIELD_FILENAME;
import static io.foojay.api.pkg.Pkg.FIELD_LATEST_BUILD_AVAILABLE;
import static io.foojay.api.util.Constants.API_VERSION_V1;
import static io.foojay.api.util.Constants.SENTINEL_PKG_ID;


public enum MongoDbManager {
    INSTANCE;

    private static final Logger        LOGGER           = LoggerFactory.getLogger(MongoDbManager.class);

    private static final String        FIELD_PACKAGE_ID = "id";
    private static final String        FIELD_DOWNLOADS  = "downloads";
    private static final String        FIELD_ID         = "id";
    private static final String        FIELD_DISTRO     = "distro";
    private static final String        FIELD_TIMESTAMP  = "timestamp";
    private static final String        FIELD_STATE      = "state";
    private static final String        FIELD_TYPE       = "type";
    private static final String        FIELD_REMOVED_AT = "removedat";

    private              MongoClient   mongoClient;
    private              boolean       connected;
    private              MongoDatabase database;


    MongoDbManager() {
        connected = false;
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

                final MongoCollection pkgsCollection = database.getCollection(Constants.PACKAGES_COLLECTION);
                if (null == pkgsCollection) {
                    LOGGER.debug("Creating mongodb collection {}", Constants.PACKAGES_COLLECTION);
                    database.createCollection(Constants.PACKAGES_COLLECTION, null);
                }
                MongoCollection downloadsCollection = database.getCollection(Constants.DOWNLOADS_COLLECTION);
                if (null == downloadsCollection) {
                    LOGGER.debug("Creating mongodb collection {}", Constants.DOWNLOADS_COLLECTION);
                    database.createCollection(Constants.DOWNLOADS_COLLECTION, null);
                }
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

    public Instant getLastDownloadForDistro(final Distro distro) {
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

        if (!collectionExists(database, Constants.DISTRO_UPDATES_COLLECTION)) {
            database.createCollection(Constants.DISTRO_UPDATES_COLLECTION);
        }

        Document document = collection.find(eq(FIELD_DISTRO, distro.getApiString())).first();
        if (null == document) {
            return Instant.ofEpochSecond(0);
        } else {
            return Instant.ofEpochSecond(document.getLong(FIELD_TIMESTAMP));
        }
    }
    public void setLastDownloadForDistro(final Distro distro) {
        connect();
        if (!connected) {
            LOGGER.debug("MongoDB not connected, last download for distro not set.");
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
        if (null == Constants.DISTRO_UPDATES_COLLECTION) {
            LOGGER.error("Constants.DISTRO_UPDATES_COLLECTION not set.");
            return;
        }
        if (!collectionExists(database, Constants.DISTRO_UPDATES_COLLECTION)) { database.createCollection(Constants.DISTRO_UPDATES_COLLECTION); }

        database.getCollection(Constants.DISTRO_UPDATES_COLLECTION)
                .updateOne(eq(FIELD_DISTRO, distro.getApiString()), combine(set(FIELD_TIMESTAMP, Instant.now().getEpochSecond())), new UpdateOptions().upsert(true));

        LOGGER.debug("Successfully updated last update for distro {}", distro.getApiString());
    }
    public boolean removeLastDownloads() {
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
