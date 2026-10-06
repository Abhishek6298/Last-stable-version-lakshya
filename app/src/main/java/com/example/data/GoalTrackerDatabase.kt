package com.example.data

data class GoalChapter(
    val sNo: Int,
    val name: String,
    val classLevel: String = "11th", // "11th" or "12th"
    val branch: String = ""
)

enum class GoalMilestone(
    val code: String,
    val label: String,
    val shortLabel: String,
    val iconEmoji: String
) {
    LECTURE("lecture", "Lecture", "Lec", "🎥"),
    UNDERSTOOD("understood", "NCERT Understood", "NCERT", "📖"),
    NOTES("notes", "Notes Prepared", "Notes", "📝"),
    REVISION_1("rev1", "Revision 1", "R1", "🔁"),
    REVISION_2("rev2", "Revision 2", "R2", "🔁"),
    REVISION_3("rev3", "Revision 3", "R3", "🔁"),
    MCQS("mcqs", "MCQs Solved", "MCQ", "❓"),
    PYQS("pyq", "PYQs Solved", "PYQ", "🎯"),
    COMPLETED("completed", "Completed ✓", "Done", "✅")
}

object GoalTrackerDatabase {

    // 1. PHYSICS (28 CHAPTERS)
    val physicsChapters = listOf(
        GoalChapter(1, "Unit and Measurements", "11th", "General Physics"),
        GoalChapter(2, "Motion in a straight line", "11th", "Mechanics"),
        GoalChapter(3, "Motion in a plane", "11th", "Mechanics"),
        GoalChapter(4, "Laws of motion", "11th", "Mechanics"),
        GoalChapter(5, "Work power and energy", "11th", "Mechanics"),
        GoalChapter(6, "Systems of particles and rotational motion", "11th", "Mechanics"),
        GoalChapter(7, "Gravitation", "11th", "Mechanics"),
        GoalChapter(8, "Mechanical Properties of Solid", "11th", "Properties of Matter"),
        GoalChapter(9, "Mechanical properties of fluids", "11th", "Properties of Matter"),
        GoalChapter(10, "Thermal Properties of Matter", "11th", "Thermal Physics"),
        GoalChapter(11, "Thermodynamics", "11th", "Thermal Physics"),
        GoalChapter(12, "Kinetic theory of gas", "11th", "Thermal Physics"),
        GoalChapter(13, "Oscillations", "11th", "Waves & Oscillations"),
        GoalChapter(14, "Waves", "11th", "Waves & Oscillations"),
        GoalChapter(15, "Electric Charges and Fields", "12th", "Electrodynamics"),
        GoalChapter(16, "Electrostatic Potential and Capacitance", "12th", "Electrodynamics"),
        GoalChapter(17, "Current Electricity", "12th", "Electrodynamics"),
        GoalChapter(18, "Moving Charges and Magnetism", "12th", "Electrodynamics"),
        GoalChapter(19, "Magnetism and Matter", "12th", "Electrodynamics"),
        GoalChapter(20, "Electromagnetic Induction", "12th", "Electrodynamics"),
        GoalChapter(21, "Alternating Current", "12th", "Electrodynamics"),
        GoalChapter(22, "Electromagnetic Waves", "12th", "Electrodynamics"),
        GoalChapter(23, "Ray Optics and Optical Instruments", "12th", "Optics"),
        GoalChapter(24, "Wave Optics", "12th", "Optics"),
        GoalChapter(25, "Dual Nature of Matter and Radiation", "12th", "Modern Physics"),
        GoalChapter(26, "Atoms", "12th", "Modern Physics"),
        GoalChapter(27, "Nuclei", "12th", "Modern Physics"),
        GoalChapter(28, "Semiconductors", "12th", "Electronics")
    )

    // 2. CHEMISTRY (22 CHAPTERS)
    val chemistryChapters = listOf(
        GoalChapter(1, "Some Basic Concepts of Chemistry", "11th", "Physical Chemistry"),
        GoalChapter(2, "Structure of Atom", "11th", "Physical Chemistry"),
        GoalChapter(3, "Classification of Elements and Periodicity in Properties", "11th", "Inorganic Chemistry"),
        GoalChapter(4, "Chemical Bonding and Molecular Structure", "11th", "Inorganic Chemistry"),
        GoalChapter(5, "Thermodynamics", "11th", "Physical Chemistry"),
        GoalChapter(6, "Equilibrium", "11th", "Physical Chemistry"),
        GoalChapter(7, "Redox Reactions", "11th", "Physical Chemistry"),
        GoalChapter(8, "The p-Block Elements (Group 13 & 14)", "11th", "Inorganic Chemistry"),
        GoalChapter(9, "Organic Chemistry - Some Basic Principles and Techniques", "11th", "Organic Chemistry"),
        GoalChapter(10, "Hydrocarbons", "11th", "Organic Chemistry"),
        GoalChapter(11, "Solution", "12th", "Physical Chemistry"),
        GoalChapter(12, "Electrochemistry", "12th", "Physical Chemistry"),
        GoalChapter(13, "Chemical Kinetics", "12th", "Physical Chemistry"),
        GoalChapter(14, "The p-Block Elements (Group 15 to 18)", "12th", "Inorganic Chemistry"),
        GoalChapter(15, "The d & f-Block Elements", "12th", "Inorganic Chemistry"),
        GoalChapter(16, "Coordination Compounds", "12th", "Inorganic Chemistry"),
        GoalChapter(17, "Haloalkanes and Haloarenes", "12th", "Organic Chemistry"),
        GoalChapter(18, "Alcohols, Phenols and Ethers", "12th", "Organic Chemistry"),
        GoalChapter(19, "Aldehydes, Ketones and Carboxylic Acids", "12th", "Organic Chemistry"),
        GoalChapter(20, "Amines", "12th", "Organic Chemistry"),
        GoalChapter(21, "Biomolecules", "12th", "Organic Chemistry"),
        GoalChapter(22, "Principles Related to Practical Chemistry", "12th", "Practical Chemistry")
    )

    // 3. BOTANY (17 CHAPTERS)
    val botanyChapters = listOf(
        GoalChapter(1, "Cell: The Unit of Life", "11th", "Cell Biology"),
        GoalChapter(2, "Cell Cycle and Cell Division", "11th", "Cell Biology"),
        GoalChapter(3, "The Living World", "11th", "Diversity of Living Organisms"),
        GoalChapter(4, "Biological Classification", "11th", "Diversity of Living Organisms"),
        GoalChapter(5, "Morphology of Flowering Plants", "11th", "Structural Organisation"),
        GoalChapter(6, "Anatomy of Flowering Plants", "11th", "Structural Organisation"),
        GoalChapter(7, "Plant Kingdom", "11th", "Diversity of Living Organisms"),
        GoalChapter(8, "Photosynthesis in Higher Plants", "11th", "Plant Physiology"),
        GoalChapter(9, "Respiration in Plants", "11th", "Plant Physiology"),
        GoalChapter(10, "Plant Growth and Development", "11th", "Plant Physiology"),
        GoalChapter(11, "Sexual Reproduction in Flowering Plants", "12th", "Reproduction"),
        GoalChapter(12, "Principles of Inheritance and Variation", "12th", "Genetics & Evolution"),
        GoalChapter(13, "Molecular Basis of Inheritance", "12th", "Genetics & Evolution"),
        GoalChapter(14, "Microbes in Human Welfare", "12th", "Biology in Human Welfare"),
        GoalChapter(15, "Organisms and Populations", "12th", "Ecology & Environment"),
        GoalChapter(16, "Ecosystem", "12th", "Ecology & Environment"),
        GoalChapter(17, "Biodiversity and Conservation", "12th", "Ecology & Environment")
    )

    // 4. ZOOLOGY (17 CHAPTERS)
    val zoologyChapters = listOf(
        GoalChapter(1, "Structural Organisation in Animals - Animal Tissues Only", "11th", "Structural Organisation"),
        GoalChapter(2, "Biomolecules", "11th", "Cell & Biomolecules"),
        GoalChapter(3, "Breathing and Exchange of Gases", "11th", "Human Physiology"),
        GoalChapter(4, "Body Fluids and Circulation", "11th", "Human Physiology"),
        GoalChapter(5, "Excretory Products and Elimination", "11th", "Human Physiology"),
        GoalChapter(6, "Locomotion and Movement", "11th", "Human Physiology"),
        GoalChapter(7, "Neural Control and Coordination", "11th", "Human Physiology"),
        GoalChapter(8, "Chemical Coordination and Integration", "11th", "Human Physiology"),
        GoalChapter(9, "Animal Kingdom (General Account & Non-Chordates)", "11th", "Animal Diversity"),
        GoalChapter(10, "Animal Kingdom (General Account & Chordates)", "11th", "Animal Diversity"),
        GoalChapter(11, "Structural Organisation in Animals - Morphology (Frog and Cockroach)", "11th", "Animal Morphology"),
        GoalChapter(12, "Human Reproduction", "12th", "Reproduction"),
        GoalChapter(13, "Reproductive Health", "12th", "Reproduction"),
        GoalChapter(14, "Evolution", "12th", "Genetics & Evolution"),
        GoalChapter(15, "Human Health and Diseases", "12th", "Health & Immunology"),
        GoalChapter(16, "Biotechnology - Principles and Process", "12th", "Biotechnology"),
        GoalChapter(17, "Biotechnology & Its Applications", "12th", "Biotechnology")
    )

    fun getChaptersForSubject(subject: String): List<GoalChapter> {
        return when (subject.lowercase()) {
            "physics" -> physicsChapters
            "chemistry" -> chemistryChapters
            "botany" -> botanyChapters
            "zoology" -> zoologyChapters
            else -> physicsChapters
        }
    }
}
