/*
 *
 *  Copyright (C) 2021, xyzsd (Zach Del)
 *
 *  Licensed under either of:
 *
 *    Apache License, Version 2.0
 *       (see LICENSE-APACHE or http://www.apache.org/licenses/LICENSE-2.0)
 *    MIT license
 *       (see LICENSE-MIT) or http://opensource.org/licenses/MIT)
 *
 *  at your option.
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *
 */

package fluent.functions;


import java.util.Locale;

/**
 * Locale-dependent bundle resources that are cached in a bundle and used by bundle functions
 * generally should be immutable. Non-immutable resources must be threadsafe.
 */
public interface FunctionResources {

    /**
     * Locale for this set of resources. This may be Locale.ROOT if not locale dependent.
     */
    Locale locale();

    /**
     * Cardinal plural selection.
     */
    // todo: this needs to be referenced due to 'implicit' call by FluentNumber
    //       so this should either be here OR possibly in the FluentBundle (or scope)
    String selectCardinal(final Number num);


}