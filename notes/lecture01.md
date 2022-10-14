Relational Model

    Entity: 'Thing' (Course as an example), uses rectangles 
        Attributes for entity (e.g. name, room, start time, end time, id), uses circles
        A key is an attribute to uniquely identify each entity in a set (id, email (id, email))
        A candidate key is a minimal collection of attributes that is a key 
            - (name, address, age) is a key but (name, address) may not be 
        Primary key: Pick a candidate key to use 

    Relationships Set between entities 
        - Enrolled connects Student and Course entities
        - Meets in connects Course and Room entities
        There could be multiple entities per relationship (0, 1, ... n)
            - A given combination of entities may appear only once
        A relationship set can have attributes
            - Key of a relationship set an be formed by taking the union of primary keys of participating entities - IT MAY NOT BE A CANDIDATE KEY IT MAY NOT BE MINIMAL
        Degree of relationship set is the number of entites connected 

    Constraints:
        - Cardinality constrait: limits the number of times that an entity can appear
            - A course can meet in at most one room (uses arrow pointing from the relationship to the entity - so we are mapping a function Course C -> Room R [where -> is "meets in"])
                - A given room can have multiple courses, but a given course can meet in one room

