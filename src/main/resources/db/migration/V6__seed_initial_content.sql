-- Initial published content for the public "About" and "Membership"
-- pages. Deliberately describes the purpose and process of the Banshawali
-- (already specified in docs/02-product-requirements.md and
-- docs/05-auth-and-verification.md) rather than any specific historical
-- claim about the Bhatta family's origin, migration, or branches -- see
-- "Do not invent historical facts" in docs/07-migration-plan.md /
-- product brief Section 7. Real family history content should be added by
-- an administrator once a content-management UI exists (Phase 6).

INSERT INTO historical_articles (slug, title_en, title_ne, body_en, body_ne, status, published_at, created_at, updated_at)
VALUES (
    'about-banshawali',
    'About the Banshawali',
    'बंशावलीको बारेमा',
    'A Banshawali is a family''s own genealogical record -- a structured account of ancestors, descendants, and the relationships that connect them across generations. This Banshawali exists to preserve the history of the Bhatta family: to record who came before us, how families branched and grew, and how today''s members connect back to earlier generations.

This information is contributed and reviewed by family members. New entries and corrections go through a review process before they become part of the permanent record, and access to private details is limited to verified family members. The goal is a record that is accurate, respectful of privacy, and useful to the family for generations to come.',
    'बंशावली भनेको परिवारको आफ्नै वंशावली अभिलेख हो — पुर्खा, सन्तान, र पुस्तौंपुस्तासम्म तिनीहरूलाई जोड्ने सम्बन्धहरूको संरचित विवरण। यो बंशावली भट्ट परिवारको इतिहास सुरक्षित राख्नका लागि अवस्थित छ: हामीभन्दा पहिले को थिए, परिवार कसरी शाखाबद्ध भई बढ्यो, र आजका सदस्यहरू पहिलेका पुस्ताहरूसँग कसरी जोडिन्छन् भन्ने कुरा अभिलेख गर्न।

यहाँको जानकारी परिवारका सदस्यहरूद्वारा योगदान गरिएको र समीक्षा गरिएको हो। नयाँ प्रविष्टि र सुधारहरू स्थायी अभिलेखको हिस्सा बन्नुअघि समीक्षा प्रक्रियाबाट गुज्र्छन्, र निजी विवरणहरूमा पहुँच प्रमाणित परिवार सदस्यहरूमा मात्र सीमित छ। लक्ष्य भनेको सही, गोपनीयतालाई सम्मान गर्ने, र आउने पुस्ताहरूका लागि उपयोगी अभिलेख तयार पार्नु हो।',
    'PUBLISHED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

INSERT INTO historical_articles (slug, title_en, title_ne, body_en, body_ne, status, published_at, created_at, updated_at)
VALUES (
    'membership-verification',
    'How Membership Verification Works',
    'सदस्यता प्रमाणीकरण कसरी काम गर्छ',
    'Access to the full family tree is limited to people who can be reasonably connected to an existing branch of the Bhatta family. When you request membership, you''ll be asked for your name, date of birth, and your father''s and grandfather''s names.

This information is compared against the existing family records to check whether it plausibly matches a known branch. Depending on how strong that match is, your request may be approved automatically, sent to an administrator for a closer look, or you may be asked for a bit more information. Either way, you''ll see the same neutral status message while your request is reviewed -- we don''t confirm or deny the existence of specific people in the tree during this process, to protect the privacy of family members who are already part of it.

Once approved, you''ll be linked to your place in the tree and can see your ancestors, relatives, and the branches of the family you belong to, subject to the privacy settings already in place for living members.',
    'सम्पूर्ण पारिवारिक वृक्षमा पहुँच भट्ट परिवारको कुनै अवस्थित शाखासँग उचित रूपमा जोडिन सक्ने व्यक्तिहरूमा मात्र सीमित छ। सदस्यताको लागि अनुरोध गर्दा, तपाईंलाई तपाईंको नाम, जन्ममिति, र तपाईंको बुबा र हजुरबुबाको नाम सोधिनेछ।

यो जानकारी अवस्थित पारिवारिक अभिलेखहरूसँग तुलना गरी कुनै ज्ञात शाखासँग मेल खान्छ कि खाँदैन जाँच गरिन्छ। यो मेल कति बलियो छ भन्नेमा भर पर्दै, तपाईंको अनुरोध स्वतः स्वीकृत हुन सक्छ, नजिकबाट हेर्नका लागि प्रशासकलाई पठाइन सक्छ, वा तपाईंसँग थप जानकारी माग्न सकिन्छ। जुनसुकै अवस्थामा पनि, तपाईंको अनुरोध समीक्षामा हुँदा तपाईंले उही तटस्थ स्थिति सन्देश देख्नुहुनेछ — यो प्रक्रियाको क्रममा हामी वृक्षमा भएका विशेष व्यक्तिहरूको अस्तित्व पुष्टि वा अस्वीकार गर्दैनौं, जसले पहिले नै सदस्य रहेका परिवारजनहरूको गोपनीयता सुरक्षित राख्छ।

स्वीकृत भएपछि, तपाईं वृक्षमा आफ्नो स्थानसँग जोडिनुहुनेछ र जीवित सदस्यहरूका लागि पहिले नै लागू गोपनीयता सेटिङहरूको अधीनमा रही आफ्ना पुर्खा, नातेदार, र आफू सम्बन्धित परिवारका शाखाहरू हेर्न सक्नुहुन्छ।',
    'PUBLISHED',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
