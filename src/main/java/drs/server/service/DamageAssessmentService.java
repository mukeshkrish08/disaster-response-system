package drs.server.service;

import drs.server.repository.DamageAssessmentRepository;
import drs.server.repository.IncidentRepository;
import drs.server.repository.UserRepository;
import drs.server.util.IdGenerator;
import drs.shared.enums.DamageLevel;
import drs.shared.enums.InfrastructureStatus;
import drs.shared.exception.ValidationException;
import drs.shared.model.DamageAssessment;
import drs.shared.model.Incident;
import drs.shared.model.User;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Records and lists damage assessments. The {@code notes} field flows
 * through {@code AesEncryption} on its way to the database - handled
 * inside {@link DamageAssessmentRepository}.
  
 */
public class DamageAssessmentService {

    private final DamageAssessmentRepository damageRepository;
    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public DamageAssessmentService(DamageAssessmentRepository damageRepository,
                                   IncidentRepository incidentRepository,
                                   UserRepository userRepository,
                                   AuditService auditService) {
        this.damageRepository = damageRepository;
        this.incidentRepository = incidentRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    /*   * Record a new damage assessment for an incident.
         * @param incidentCode      target incident
     * @param assessorUserCode  user performing the assessment
     * @param buildingDamage    building damage level (required)
     * @param roadStatus        road infrastructure status (required)
     * @param powerStatus       power infrastructure status (required)
     * @param waterStatus       water infrastructure status (required)
     * @param casualtyEstimate  estimated casualties (>= 0)
     * @param notes             free-text notes (encrypted at rest)
     * @param clientIp          remote address
     * @return the persisted DamageAssessment
     */
    public DamageAssessment recordAssessment(String incidentCode,
                                             String assessorUserCode,
                                             DamageLevel buildingDamage,
                                             InfrastructureStatus roadStatus,
                                             InfrastructureStatus powerStatus,
                                             InfrastructureStatus waterStatus,
                                             int casualtyEstimate,
                                             String notes, String clientIp) {
        if (buildingDamage == null || roadStatus == null
                || powerStatus == null || waterStatus == null) {
            throw new ValidationException(
                    "All four status fields are required.");
        }
        if (casualtyEstimate < 0) {
            throw new ValidationException(
                    "Casualty estimate cannot be negative.");
        }
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new ValidationException(
                        "Incident not found: " + incidentCode));
        User assessor = userRepository.findByCode(assessorUserCode)
                .orElseThrow(() -> new ValidationException(
                        "Assessor user not found"));

        DamageAssessment a = new DamageAssessment();
        a.setAssessmentCode(IdGenerator.generateAssessmentCode());
        a.setIncidentPk(incident.getIncidentPk());
        a.setAssessedByUserPk(assessor.getUserPk());
        a.setBuildingDamageLevel(buildingDamage);
        a.setRoadStatus(roadStatus);
        a.setPowerStatus(powerStatus);
        a.setWaterStatus(waterStatus);
        a.setCasualtyEstimate(casualtyEstimate);
        a.setNotes(notes);
        a.setAssessedAt(LocalDateTime.now());

        damageRepository.save(a);

        auditService.logAction(assessor.getUserPk(),
                "RECORD_DAMAGE_ASSESSMENT", "DamageAssessment",
                a.getAssessmentCode(),
                "building=" + buildingDamage + ", casualties=" + casualtyEstimate,
                clientIp, true);
        return a;
    }

    public List<DamageAssessment> listAssessmentsForIncident(String incidentCode) {
        Incident incident = incidentRepository.findByCode(incidentCode)
                .orElseThrow(() -> new ValidationException(
                        "Incident not found: " + incidentCode));
        return damageRepository.findByIncidentPk(incident.getIncidentPk());
    }
}
