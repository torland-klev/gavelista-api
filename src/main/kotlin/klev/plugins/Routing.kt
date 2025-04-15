package klev.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import klev.db.groups.GroupsRoutes
import klev.db.groups.memberships.GroupMembershipRoutes
import klev.db.images.ImageRoutes
import klev.db.users.UserRoutes
import klev.db.wishes.Occasion
import klev.db.wishes.Status
import klev.db.wishes.WishesRoutes
import klev.mainHtml

fun Application.configureRouting(
    wishesRoutes: WishesRoutes,
    groupsRoutes: GroupsRoutes,
    groupMembershipRoutes: GroupMembershipRoutes,
    userRoutes: UserRoutes,
    imageRoutes: ImageRoutes,
) {
    routing {
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")
        get("/") {
            mainHtml()
        }
        get("/token") {
            val userSession = getSession(call)
            if (userSession != null) {
                call.respondText(userSession.session.token)
            } else {
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
        get("/wishes/status") {
            call.respond(Status.entries.map { it.name })
        }
        get("/wishes/occasion") {
            call.respond(Occasion.entries.map { it.name })
        }
        get("/view-image/{id}") {
            val userSession = getSession(call)
            if (userSession != null) {
                imageRoutes.getById(call, userSession)
            }
        }
        authenticate("auth-bearer") {
            route("/me") {
                get {
                    userRoutes.me(call)
                }
                put {
                    userRoutes.updateById(call)
                }
            }
            route("/groups") {
                get {
                    groupsRoutes.all(call)
                }
                post {
                    groupsRoutes.post(call)
                }
                route("/{groupId}") {
                    get {
                        groupsRoutes.get(call)
                    }
                    delete {
                        groupsRoutes.deleteIfOwner(call)
                    }
                    patch {
                        groupsRoutes.updateIfAdmin(call)
                    }
                    post("/invite") {
                        groupsRoutes.inviteIfAdmin(call)
                    }
                    get("/role") {
                        groupsRoutes.getRoleInGroup(call)
                    }
                    get("/isMember") {
                        groupsRoutes.isInGroup(call)
                    }
                    route("/members") {
                        get {
                            groupMembershipRoutes.allByGroup(call)
                        }
                        get("/{memberId}") {
                            groupMembershipRoutes.get(call)
                        }
                        get("/{memberId}/wishes") {
                            groupsRoutes.wishesForMember(call)
                        }
                        delete("/{memberId}") {
                            groupMembershipRoutes.deleteIfCanAdmin(call)
                        }
                        post("/{memberId}") {
                            groupMembershipRoutes.addUserToGroup(call)
                        }
                        post("/join") {
                            groupsRoutes.joinGroup(call)
                        }
                        route("/admins") {
                            get {
                                groupMembershipRoutes.getAdmins(call)
                            }
                            get("/{memberId}") {
                                groupMembershipRoutes.getAdminById(call)
                            }
                            post("/{memberId}") {
                                groupMembershipRoutes.makeAdmin(call)
                            }
                            delete("/{memberId}") {
                                groupMembershipRoutes.removeAsAdmin(call)
                            }
                        }
                    }
                    route("/wishes") {
                        get {
                            groupsRoutes.allWishes(call)
                        }
                        post {
                            wishesRoutes.postForGroup(call)
                        }
                    }
                }
            }
            route("/users") {
                get {
                    userRoutes.get(call)
                }
                route("/{userId}") {
                    get {
                        userRoutes.getById(call)
                    }
                    route("/wishes") {
                        get {
                            wishesRoutes.allUserHasCreated(call)
                        }
                    }
                    route("/groups") {
                        get {
                            groupMembershipRoutes.allByUser(call)
                        }
                    }
                }
            }
            route("/wishes") {
                get {
                    wishesRoutes.allUserHasReadAccessTo(call)
                }
                post {
                    wishesRoutes.post(call)
                }
                route("/{id}") {
                    get {
                        wishesRoutes.getId(call)
                    }
                    delete {
                        wishesRoutes.delete(call)
                    }
                    patch {
                        wishesRoutes.patch(call)
                    }
                    route("/groups") {
                        get {
                            wishesRoutes.getGroupsForWish(call)
                        }
                    }
                }
            }
            route("/images") {
                post {
                    imageRoutes.post(call)
                }
                route("/{id}") {
                    get {
                        imageRoutes.getById(call)
                    }
                    delete {
                        imageRoutes.deleteById(call)
                    }
                }
            }
        }
    }
}
