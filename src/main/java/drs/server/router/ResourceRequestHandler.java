package drs.server.router;

import drs.server.ServerApplicationContext;
import drs.server.Session;
import drs.shared.enums.ResourceType;
import drs.shared.enums.UserRole;
import drs.shared.model.Resource;
import drs.shared.model.ResourceAllocation;
import drs.shared.protocol.OperationType;
import drs.shared.protocol.ProtocolConstants;
import drs.shared.protocol.Request;
import drs.shared.protocol.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles resource inventory + allocation operations (Feature 1).
  
 */
public class ResourceRequestHandler extends AbstractRequestHandler {

    public ResourceRequestHandler(ServerApplicationContext context) {
        super(context);
    }

    @Override
    public Response handle(Request request, Session session) {
        OperationType op = request.getOperation();
        switch (op) {
            case LIST_RESOURCES:                 return listResources(request, session);
            case ADD_RESOURCE:                   return addResource(request, session);
            case ALLOCATE_RESOURCE:              return allocate(request, session);
            case RETURN_ALLOCATION:              return returnAlloc(request, session);
            case LIST_ALLOCATIONS_FOR_INCIDENT:  return listAllocationsFor(request, session);
            case SEND_RESOURCE_TO_MAINTENANCE:   return sendToMaintenance(request, session);
            case RETURN_RESOURCE_FROM_MAINTENANCE: return returnFromMaintenance(request, session);
            case RETIRE_RESOURCE:                return retire(request, session);
            default:
                return error(ProtocolConstants.ERR_UNKNOWN_OPERATION,
                        "Unsupported operation: " + op);
        }
    }

    private Response listResources(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR,
                UserRole.TEAM_LEADER, UserRole.ADMIN);
        String typeName = request.get("resourceType");
        ResourceType type = (typeName == null || typeName.isEmpty())
                ? null : ResourceType.valueOf(typeName);
        List<Resource> list = (type == null)
                ? context.getResourceService().listResources()
                : context.getResourceService().listAvailableResources(type);
        return ok(new ArrayList<>(list));
    }

    private Response addResource(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.ADMIN, UserRole.COORDINATOR);
        String name = request.get("resourceName");
        ResourceType type = ResourceType.valueOf((String) request.get("resourceType"));
        Number qty = request.get("quantityTotal");
        String homeLocationCode = request.get("homeLocationCode");
        Resource r = context.getResourceService().addResource(name, type,
                qty == null ? 0 : qty.intValue(), homeLocationCode,
                session.getUserCode(), session.getClientIp());
        return ok(r);
    }

    private Response allocate(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR, UserRole.ADMIN);
        String resourceCode = request.get("resourceCode");
        String incidentCode = request.get("incidentCode");
        Number quantity = request.get("quantity");
        String notes = request.get("notes");
        ResourceAllocation alloc = context.getResourceService().allocateResource(
                resourceCode, incidentCode,
                quantity == null ? 0 : quantity.intValue(),
                session.getUserCode(), notes, session.getClientIp());
        return ok(alloc);
    }

    private Response returnAlloc(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.COORDINATOR, UserRole.ADMIN);
        String code = request.get("allocationCode");
        context.getResourceService().returnAllocation(code,
                session.getUserCode(), session.getClientIp());
        return ok();
    }

    private Response listAllocationsFor(Request request, Session session) {
        authz.requireAuthenticated(session);
        String code = request.get("incidentCode");
        List<ResourceAllocation> list = context.getResourceService()
                .listAllocationsForIncident(code);
        return ok(new ArrayList<>(list));
    }

    // -----------------------------------------------------------------
    // Resource lifecycle handlers - open to ADMIN and COORDINATOR.
    // Server-side state checks live in ResourceService.
    // -----------------------------------------------------------------

    private Response sendToMaintenance(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.ADMIN, UserRole.COORDINATOR);
        String code = request.get("resourceCode");
        if (code == null || code.isBlank()) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "resourceCode is required.");
        }
        Resource r = context.getResourceService().sendToMaintenance(
                code, session.getUserCode(), session.getClientIp());
        return ok(java.util.Map.of("resource", r));
    }

    private Response returnFromMaintenance(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.ADMIN, UserRole.COORDINATOR);
        String code = request.get("resourceCode");
        if (code == null || code.isBlank()) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "resourceCode is required.");
        }
        Resource r = context.getResourceService().returnFromMaintenance(
                code, session.getUserCode(), session.getClientIp());
        return ok(java.util.Map.of("resource", r));
    }

    private Response retire(Request request, Session session) {
        authz.requireAnyRole(session, UserRole.ADMIN, UserRole.COORDINATOR);
        String code = request.get("resourceCode");
        if (code == null || code.isBlank()) {
            return error(ProtocolConstants.ERR_VALIDATION,
                    "resourceCode is required.");
        }
        Resource r = context.getResourceService().retireResource(
                code, session.getUserCode(), session.getClientIp());
        return ok(java.util.Map.of("resource", r));
    }
}
