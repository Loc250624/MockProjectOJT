package com.ojtsu26.elearning.mapper;

import com.ojtsu26.elearning.dto.request.RoadmapRequestDTO;
import com.ojtsu26.elearning.dto.response.RoadmapResponseDTO;
import com.ojtsu26.elearning.model.entity.Roadmap;
import com.ojtsu26.elearning.model.entity.User;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-06-29T22:09:51+0700",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.11 (Oracle Corporation)"
)
@Component
public class RoadmapMapperImpl implements RoadmapMapper {

    @Override
    public RoadmapResponseDTO toDto(Roadmap entity) {
        if ( entity == null ) {
            return null;
        }

        RoadmapResponseDTO roadmapResponseDTO = new RoadmapResponseDTO();

        roadmapResponseDTO.setInstructorId( entityInstructorId( entity ) );
        roadmapResponseDTO.setId( entity.getId() );
        roadmapResponseDTO.setTitle( entity.getTitle() );
        roadmapResponseDTO.setDescription( entity.getDescription() );
        roadmapResponseDTO.setCreatedAt( entity.getCreatedAt() );

        return roadmapResponseDTO;
    }

    @Override
    public Roadmap toEntity(RoadmapRequestDTO dto) {
        if ( dto == null ) {
            return null;
        }

        Roadmap.RoadmapBuilder roadmap = Roadmap.builder();

        roadmap.instructor( roadmapRequestDTOToUser( dto ) );
        roadmap.title( dto.getTitle() );
        roadmap.description( dto.getDescription() );

        return roadmap.build();
    }

    private Integer entityInstructorId(Roadmap roadmap) {
        if ( roadmap == null ) {
            return null;
        }
        User instructor = roadmap.getInstructor();
        if ( instructor == null ) {
            return null;
        }
        Integer id = instructor.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    protected User roadmapRequestDTOToUser(RoadmapRequestDTO roadmapRequestDTO) {
        if ( roadmapRequestDTO == null ) {
            return null;
        }

        User.UserBuilder user = User.builder();

        user.id( roadmapRequestDTO.getInstructorId() );

        return user.build();
    }
}
