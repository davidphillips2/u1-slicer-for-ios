#include "../include/sapil.h"
#include "sapil_internal.h"
#include "libslic3r/Model.hpp"

// =============================================================================
// sapil_arrange.cpp — Multiple copy instance placement
// =============================================================================

namespace sapil {

// Forward declarations from sapil_model.cpp
extern Slic3r::Model& getGlobalModel();
extern bool isModelLoaded();

bool SlicerEngine::setModelInstances(const std::vector<std::pair<float, float>>& positions) {
    if (!isModelLoaded()) {
        SAPIL_LOGE("setModelInstances: no model loaded");
        return false;
    }
    if (positions.empty()) {
        SAPIL_LOGE("setModelInstances: no positions provided");
        return false;
    }

    Slic3r::Model& model = getGlobalModel();

    for (auto* obj : model.objects) {
        obj->clear_instances();
        for (const auto& pos : positions) {
            auto* inst = obj->add_instance();
            // Z=0; ensure_on_bed() will lower it onto the print surface at slice time
            inst->set_offset(Slic3r::Vec3d(
                static_cast<double>(pos.first),
                static_cast<double>(pos.second),
                0.0
            ));
        }
    }

    SAPIL_LOGI("Set %d instance(s) across %d object(s)",
        (int)positions.size(), (int)model.objects.size());
    return true;
}

} // namespace sapil
